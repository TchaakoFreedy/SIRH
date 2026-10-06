package com.fric.sirh.controller;

import com.fric.sirh.dto.PosteRequest;
import com.fric.sirh.model.Departement;
import com.fric.sirh.model.Poste;
import com.fric.sirh.model.User;
import com.fric.sirh.service.DepartementService;
import com.fric.sirh.service.PosteService;
import com.fric.sirh.service.UserService;
import com.fric.sirh.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/postes")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PosteController {

    private final PosteService posteService;
    private final DepartementService departementService;
    private final UserService userService; // Ajouté pour la recherche d'utilisateur

    // ==========================================
    // ✅ UTILITY METHODS
    // ==========================================

    /**
     * Récupère l'ID MongoDB de l'utilisateur connecté.
     * Si le principal est CustomUserDetails, retourne son ID.
     * Sinon, tente de trouver l'utilisateur par email (en normalisant la casse).
     */
    private String getCurrentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }
        // Fallback: si le principal est un String (email ou ID)
        String identifier = authentication.getName();
        log.warn("Le principal n'est pas CustomUserDetails, tentative de recherche par identifiant: {}", identifier);

        try {
            // Essayer de trouver par email (normalisé en minuscules)
            User user = userService.findByEmail(identifier.toLowerCase());
            if (user != null) {
                log.debug("Utilisateur trouvé par email: {}", user.getId());
                return user.getId();
            }
        } catch (Exception e) {
            log.debug("Recherche par email échouée: {}", e.getMessage());
        }

        // Si l'identifiant ressemble à un ObjectId (24 caractères hex), essayer par ID
        if (identifier.matches("^[a-fA-F0-9]{24}$")) {
            try {
                User user = userService.findById(identifier);
                if (user != null) {
                    log.debug("Utilisateur trouvé par ID: {}", user.getId());
                    return user.getId();
                }
            } catch (Exception e) {
                log.debug("Recherche par ID échouée: {}", e.getMessage());
            }
        }

        // Dernier recours : retourner l'identifiant original (peut causer des erreurs)
        log.warn("Impossible de trouver l'utilisateur avec l'identifiant: {}, retour de l'identifiant brut", identifier);
        return identifier;
    }

    /**
     * Vérifie si l'utilisateur possède un rôle donné.
     */
    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role) ||
                        a.getAuthority().equals("ROLE_" + role));
    }

    // ==========================================
    // ✅ CREATE
    // ==========================================
    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<?> create(@RequestBody PosteRequest request) {
        try {
            Departement departement = null;
            if (request.getDepartementId() != null && !request.getDepartementId().isEmpty()) {
                departement = departementService.getById(request.getDepartementId());
                if (departement == null) {
                    return ResponseEntity.badRequest().body("Département non trouvé pour l'ID : " + request.getDepartementId());
                }
            }

            Poste poste = new Poste();
            poste.setCode(request.getCode());
            poste.setLibelle(request.getLibelle());
            poste.setDescription(request.getDescription());
            poste.setDepartement(departement);

            Poste created = posteService.create(poste);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // ✅ GET ALL - AVEC FILTRAGE PAR RÔLE
    // ==========================================
    @GetMapping
    @PreAuthorize("hasAuthority('POSITION_VIEW_ALL') or hasAuthority('POSITION_VIEW')")
    public ResponseEntity<List<Poste>> getAll(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("🔍 Récupération des postes pour l'utilisateur: {}", userId);

        try {
            boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
            boolean isDirection = hasRole(authentication, "DIRECTION");
            boolean isTopManager = hasRole(authentication, "TOP_MANAGER");

            List<Poste> postes;

            if (isRH || isTopManager) {
                postes = posteService.getAll();
                log.info("✅ Admin/RH/TopManager - {} postes retournés", postes.size());
            } else if (isDirection) {
                postes = posteService.getPostesByUserCompany(userId);
                log.info("🎯 Direction {} - {} postes retournés de son entreprise",
                        userId, postes.size());
            } else {
                Departement userDept = departementService.getDepartementByUserId(userId);
                if (userDept != null) {
                    postes = posteService.getByDepartement(userDept.getId());
                    log.info("👤 Utilisateur {} - {} postes retournés de son département",
                            userId, postes.size());
                } else {
                    postes = List.of();
                    log.warn("⚠️ Utilisateur {} - Aucun département trouvé", userId);
                }
            }

            return ResponseEntity.ok(postes);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des postes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // ✅ GET ACTIVE - AVEC FILTRAGE PAR RÔLE
    // ==========================================
    @GetMapping("/active")
    @PreAuthorize("hasAuthority('POSITION_VIEW_ALL') or hasAuthority('POSITION_VIEW')")
    public ResponseEntity<List<Poste>> getActive(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("🔍 Récupération des postes actifs pour l'utilisateur: {}", userId);

        try {
            boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
            boolean isDirection = hasRole(authentication, "DIRECTION");
            boolean isTopManager = hasRole(authentication, "TOP_MANAGER");

            List<Poste> postes;

            if (isRH || isTopManager) {
                postes = posteService.getActivePostes();
            } else if (isDirection) {
                postes = posteService.getActivePostesByUserCompany(userId);
            } else {
                Departement userDept = departementService.getDepartementByUserId(userId);
                if (userDept != null) {
                    postes = posteService.getByDepartement(userDept.getId()).stream()
                            .filter(Poste::isActive)
                            .toList();
                } else {
                    postes = List.of();
                }
            }

            log.info("✅ {} postes actifs retournés", postes.size());
            return ResponseEntity.ok(postes);

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // ✅ GET BY ID - AVEC VÉRIFICATION
    // ==========================================
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('POSITION_VIEW')")
    public ResponseEntity<?> getById(@PathVariable String id, Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("🔍 Récupération du poste ID: {}", id);

        try {
            Optional<Poste> posteOpt = posteService.getById(id);
            if (posteOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Poste poste = posteOpt.get();

            boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
            boolean isDirection = hasRole(authentication, "DIRECTION");
            boolean isTopManager = hasRole(authentication, "TOP_MANAGER");

            if (isDirection) {
                boolean hasAccess = posteService.isPosteInUserCompany(userId, id);
                if (!hasAccess) {
                    log.warn("⚠️ Direction {} - Accès refusé au poste {}", userId, id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            if (!isRH && !isDirection && !isTopManager) {
                Departement userDept = departementService.getDepartementByUserId(userId);
                if (userDept == null ||
                        poste.getDepartement() == null ||
                        !userDept.getId().equals(poste.getDepartement().getId())) {
                    log.warn("⚠️ Utilisateur {} - Accès refusé au poste {}", userId, id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            return ResponseEntity.ok(poste);

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // ✅ GET BY DEPARTEMENT - AVEC VÉRIFICATION
    // ==========================================
    @GetMapping("/departement/{departementId}")
    @PreAuthorize("hasAuthority('POSITION_VIEW_ALL') or hasAuthority('POSITION_VIEW')")
    public ResponseEntity<List<Poste>> getByDepartement(
            @PathVariable String departementId,
            Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("🔍 Récupération des postes du département: {}", departementId);

        try {
            boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
            boolean isDirection = hasRole(authentication, "DIRECTION");
            boolean isTopManager = hasRole(authentication, "TOP_MANAGER");

            if (isDirection) {
                boolean hasAccess = departementService.isDepartementInUserCompany(userId, departementId);
                if (!hasAccess) {
                    log.warn("⚠️ Direction {} - Accès refusé au département {}", userId, departementId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            if (!isRH && !isDirection && !isTopManager) {
                Departement userDept = departementService.getDepartementByUserId(userId);
                if (userDept == null || !userDept.getId().equals(departementId)) {
                    log.warn("⚠️ Utilisateur {} - Accès refusé au département {}", userId, departementId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            List<Poste> postes = posteService.getByDepartement(departementId);
            log.info("✅ {} postes trouvés dans le département", postes.size());
            return ResponseEntity.ok(postes);

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // ✅ UPDATE
    // ==========================================
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<?> update(
            @PathVariable String id,
            @RequestBody Poste poste) {
        try {
            Poste updated = posteService.update(id, poste);
            return ResponseEntity.ok(updated);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // ✅ DELETE
    // ==========================================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<?> delete(@PathVariable String id) {
        try {
            posteService.delete(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ==========================================
    // ✅ TOGGLE ACTIVE
    // ==========================================
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<?> toggleActive(@PathVariable String id) {
        try {
            Poste toggled = posteService.toggleActive(id);
            return ResponseEntity.ok(toggled);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
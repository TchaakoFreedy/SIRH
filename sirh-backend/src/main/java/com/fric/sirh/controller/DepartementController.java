package com.fric.sirh.controller;

import com.fric.sirh.model.Departement;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.security.CustomUserDetails;
import com.fric.sirh.service.DepartementService;
import com.fric.sirh.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/departements")
@CrossOrigin("*")
@RequiredArgsConstructor
public class DepartementController {

    private final DepartementService service;
    private final UserService userService;

    // ==========================================
    // ✅ UTILITY METHODS
    // ==========================================

    /**
     * Récupère l'ID MongoDB de l'utilisateur connecté.
     * Si le principal est CustomUserDetails, retourne son ID.
     * Sinon, tente de trouver l'utilisateur par email ou ID.
     */
    private String getCurrentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }
        String identifier = authentication.getName();
        log.warn("Le principal n'est pas CustomUserDetails, tentative de recherche par identifiant: {}", identifier);

        try {
            // Recherche par email (normalisé en minuscules)
            User user = userService.findByEmail(identifier.toLowerCase());
            if (user != null) {
                return user.getId();
            }
        } catch (Exception e) {
            log.debug("Recherche par email échouée: {}", e.getMessage());
        }

        // Si l'identifiant ressemble à un ObjectId, recherche par ID
        if (identifier.matches("^[a-fA-F0-9]{24}$")) {
            try {
                User user = userService.findById(identifier);
                if (user != null) {
                    return user.getId();
                }
            } catch (Exception e) {
                log.debug("Recherche par ID échouée: {}", e.getMessage());
            }
        }

        // Dernier recours : retourner l'identifiant brut (peut causer des erreurs)
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
    // ✅ CRÉATION
    // ==========================================
    @PostMapping
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<Departement> create(@RequestBody Departement d) {
        log.info("📝 Création d'un département: {}", d.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(d));
    }

    // ==========================================
    // ✅ GET ALL - AVEC FILTRAGE PAR RÔLE
    // ==========================================
    @GetMapping
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW_ALL') or hasAuthority('DEPARTMENT_VIEW')")
    public ResponseEntity<List<Departement>> getAll(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("🔍 Récupération des départements pour l'utilisateur: {}", userId);

        try {
            boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
            boolean isDirection = hasRole(authentication, "DIRECTION");
            boolean isTopManager = hasRole(authentication, "TOP_MANAGER");
            boolean isEmployee = hasRole(authentication, "EMPLOYEE");

            List<Departement> departements;

            if (isRH || isTopManager) {
                departements = service.getAll();
                log.info("✅ Admin/RH/TOP_MANAGER - {} départements retournés", departements.size());
            } else if (isDirection) {
                departements = service.getDepartementsByUserCompany(userId);
                log.info("🎯 Direction {} - {} départements retournés de son entreprise",
                        userId, departements.size());
            } else {
                // Employee ou Manager - voit son propre département
                Departement dept = service.getDepartementByUserId(userId);
                departements = dept != null ? List.of(dept) : List.of();
                log.info("👤 Utilisateur {} - {} département(s) retourné(s)", userId, departements.size());
            }

            return ResponseEntity.ok(departements);

        } catch (Exception e) {
            log.error("❌ Erreur lors de la récupération des départements: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // ✅ GET BY ID - AVEC VÉRIFICATION
    // ==========================================
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW') or hasAuthority('DEPARTMENT_VIEW_ALL')")
    public ResponseEntity<Departement> getById(@PathVariable String id, Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("🔍 Récupération du département ID: {}", id);

        try {
            boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
            boolean isDirection = hasRole(authentication, "DIRECTION");
            boolean isTopManager = hasRole(authentication, "TOP_MANAGER");
            boolean isEmployee = hasRole(authentication, "EMPLOYEE");

            Departement departement = service.getById(id);

            if (isRH || isTopManager) {
                log.info("✅ RH/TOP_MANAGER - Accès autorisé au département {}", id);
                return ResponseEntity.ok(departement);
            }

            if (isDirection) {
                boolean hasAccess = service.isDepartementInUserCompany(userId, id);
                if (!hasAccess) {
                    log.warn("⚠️ Direction {} - Accès refusé au département {}", userId, id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
                log.info("✅ Direction - Accès autorisé au département {}", id);
                return ResponseEntity.ok(departement);
            }

            // Manager ou Employee - ne voit que son département
            Departement userDept = service.getDepartementByUserId(userId);
            if (userDept == null || !userDept.getId().equals(id)) {
                log.warn("⚠️ Utilisateur {} - Accès refusé au département {}", userId, id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            log.info("✅ Département trouvé: {}", departement.getName());
            return ResponseEntity.ok(departement);

        } catch (Exception e) {
            log.error("❌ Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // ✅ GET BY EMPLOYEE ID
    // ==========================================
    @GetMapping("/by-employee/{employeeId}")
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    public ResponseEntity<Departement> getByEmployeeId(@PathVariable String employeeId) {
        log.info("🔍 GET /by-employee/{} - Récupération du département pour l'employé", employeeId);
        Departement dept = service.getDepartementByEmployeeId(employeeId);
        if (dept == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(dept);
    }

    // ==========================================
    // ✅ GET MY DEPARTEMENT
    // ==========================================
    @GetMapping("/my-departement")
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW')")
    public ResponseEntity<Departement> getMyDepartement(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("🔍 GET /my-departement - Récupération du département pour l'utilisateur connecté: {}", userId);
        Departement dept = service.getDepartementByUserId(userId);
        if (dept == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        return ResponseEntity.ok(dept);
    }

    // ==========================================
    // ✅ GET BY ENTREPRISE - AVEC VÉRIFICATION
    // ==========================================
    @GetMapping("/entreprise/{entrepriseId}")
    @PreAuthorize("hasAuthority('DEPARTMENT_VIEW_ALL') or hasAuthority('COMPANY_VIEW')")
    public ResponseEntity<List<Departement>> byEntreprise(
            @PathVariable String entrepriseId,
            Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("🔍 Récupération des départements pour l'entreprise: {}", entrepriseId);

        boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
        boolean isTopManager = hasRole(authentication, "TOP_MANAGER");
        boolean isDirection = hasRole(authentication, "DIRECTION");
        boolean isEmployee = hasRole(authentication, "EMPLOYEE");

        if (isEmployee) {
            log.warn("⚠️ EMPLOYEE - Accès refusé à la liste des départements par entreprise");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!isRH && !isTopManager && isDirection) {
            String userCompanyId = service.getCompanyIdByUserId(userId);
            if (!entrepriseId.equals(userCompanyId)) {
                log.warn("⚠️ Direction - Accès refusé à une autre entreprise: {}", entrepriseId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        List<Departement> departements = service.getByEntreprise(entrepriseId);
        log.info("✅ {} départements trouvés", departements.size());
        return ResponseEntity.ok(departements);
    }

    // ==========================================
    // ✅ UPDATE
    // ==========================================
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<Departement> update(@PathVariable String id, @RequestBody Departement d) {
        log.info("📝 Mise à jour du département ID: {}", id);
        return ResponseEntity.ok(service.update(id, d));
    }

    // ==========================================
    // ✅ SUSPENDRE
    // ==========================================
    @PutMapping("/{id}/suspendre")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<Departement> suspendre(@PathVariable String id) {
        log.info("⛔ Suspension du département ID: {}", id);
        return ResponseEntity.ok(service.suspendre(id));
    }

    // ==========================================
    // ✅ RÉACTIVER
    // ==========================================
    @PutMapping("/{id}/reactiver")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH')")
    public ResponseEntity<Departement> reactiver(@PathVariable String id) {
        log.info("✅ Réactivation du département ID: {}", id);
        return ResponseEntity.ok(service.reactiver(id));
    }

    // ==========================================
    // ✅ AFFECTER EMPLOYÉ
    // ==========================================
    @PutMapping("/{departementId}/affecter/{employeeId}")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN') or hasAuthority('RH') or hasAuthority('MANAGER')")
    public ResponseEntity<Employee> affecter(
            @PathVariable String departementId,
            @PathVariable String employeeId,
            Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("📌 Affectation de l'employé {} au département {}", employeeId, departementId);

        boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
        boolean isDirection = hasRole(authentication, "DIRECTION");
        boolean isTopManager = hasRole(authentication, "TOP_MANAGER");
        boolean isEmployee = hasRole(authentication, "EMPLOYEE");

        if (isEmployee || isTopManager) {
            log.warn("⚠️ Accès refusé pour l'affectation (rôle non autorisé)");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (isDirection) {
            boolean hasAccess = service.isDepartementInUserCompany(userId, departementId);
            if (!hasAccess) {
                log.warn("⚠️ Direction - Accès refusé pour affectation au département {}", departementId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(service.affecterEmploye(employeeId, departementId));
    }
}
// src/main/java/com/fric/sirh/controller/ContratController.java
package com.fric.sirh.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fric.sirh.dto.*;
import com.fric.sirh.exception.ContractConflictException;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.service.ContratExpirationScheduler;
import com.fric.sirh.service.ContratService;
import com.fric.sirh.service.EmployeeService;
import com.fric.sirh.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/contrats")
@RequiredArgsConstructor

public class ContratController {

    private final ContratService contratService;
    private final ContratExpirationScheduler contratExpirationScheduler;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    private final EmployeeService employeeService;

    // ==========================================
    // GESTION DES EXCEPTIONS
    // ==========================================
    @ExceptionHandler(ContractConflictException.class)
    public ResponseEntity<Map<String, String>> handleContractConflict(ContractConflictException ex) {
        log.warn("Conflit contrat: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("message", ex.getMessage()));
    }

    // ==========================================
    // CREER UN CONTRAT AVEC OU SANS IMAGES
    // ==========================================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<ContratDTO> creerContrat(
            @RequestPart("contrat") String contratJson,
            @RequestPart(value = "files", required = false) MultipartFile[] files) {

        String currentUser = getCurrentUser();

        log.info("=== CREATION CONTRAT AVEC IMAGES ===");
        log.info("JSON recu: {}", contratJson);

        CreateContratRequest request = null;
        try {
            request = objectMapper.readValue(contratJson, CreateContratRequest.class);
            log.info("Contrat parse: employeeId={}, type={}, dateDebut={}",
                    request.getEmployeeId(), request.getTypeContrat(), request.getDateDebut());
        } catch (Exception e) {
            log.error("Erreur lors du parsing du JSON", e);
            throw new RuntimeException("Erreur lors du parsing du contrat: " + e.getMessage());
        }

        if (files != null && files.length > 0) {
            log.info("Nombre de fichiers recus: {}", files.length);
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                log.info("  - Fichier {}: {}, Taille: {} bytes, Type: {}",
                        i + 1, file.getOriginalFilename(), file.getSize(), file.getContentType());
            }
        } else {
            log.info("Aucun fichier recu");
        }

        ContratDTO contrat = contratService.creerContrat(request, files, currentUser);

        log.info("Contrat cree avec succes: ID={}, Images={}",
                contrat.getId(), contrat.getImageUrls() != null ? contrat.getImageUrls().size() : 0);

        return ResponseEntity.status(HttpStatus.CREATED).body(contrat);
    }

    // ==========================================
    // UPLOAD IMAGES POUR UN CONTRAT EXISTANT
    // ==========================================
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<ContratDTO> uploadContratImages(
            @PathVariable String id,
            @RequestParam("files") MultipartFile[] files) {

        String currentUser = getCurrentUser();

        log.info("Upload images pour le contrat ID: {}", id);
        log.info("Nombre de fichiers recus: {}", files != null ? files.length : 0);

        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                MultipartFile file = files[i];
                log.info("  - Fichier {}: {}, Taille: {} bytes",
                        i + 1, file.getOriginalFilename(), file.getSize());
            }
        }

        ContratDTO contrat = contratService.uploadContratImages(id, files, currentUser);

        log.info("Images uploadées: {} images",
                contrat.getImageUrls() != null ? contrat.getImageUrls().size() : 0);

        return ResponseEntity.ok(contrat);
    }

    // ==========================================
    // MODIFIER UN CONTRAT
    // ==========================================
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<ContratDTO> modifierContrat(
            @PathVariable String id,
            @Valid @RequestBody UpdateContratRequest request) {
        String currentUser = getCurrentUser();
        ContratDTO contrat = contratService.modifierContrat(id, request, currentUser);
        return ResponseEntity.ok(contrat);
    }

    // ==========================================
    // RENOUVELER UN CONTRAT
    // ==========================================
    @PostMapping("/renouveler")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<ContratDTO> renouvelerContrat(@Valid @RequestBody RenouvellementContratRequest request) {
        String currentUser = getCurrentUser();
        ContratDTO contrat = contratService.renouvelerContrat(request, currentUser);
        return ResponseEntity.ok(contrat);
    }

    // ==========================================
    // RESILIER UN CONTRAT
    // ==========================================
    @PatchMapping("/{id}/resilier")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<ContratDTO> resilierContrat(@PathVariable String id) {
        String currentUser = getCurrentUser();
        ContratDTO contrat = contratService.resillierContrat(id, currentUser);
        return ResponseEntity.ok(contrat);
    }

    // ==========================================
    // ARCHIVER UN CONTRAT
    // ==========================================
    @PatchMapping("/{id}/archiver")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<ContratDTO> archiverContrat(@PathVariable String id) {
        String currentUser = getCurrentUser();
        ContratDTO contrat = contratService.archiverContrat(id, currentUser);
        return ResponseEntity.ok(contrat);
    }

    // ==========================================
    // GESTION DES IMAGES - CONSULTATION
    // ==========================================
    @GetMapping("/{id}/images")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public ResponseEntity<List<String>> getContratImages(@PathVariable String id) {
        return ResponseEntity.ok(contratService.getContratImages(id));
    }

    @GetMapping("/{id}/download")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public ResponseEntity<byte[]> downloadContratImage(
            @PathVariable String id,
            @RequestParam("index") int index) {
        return contratService.downloadContratImage(id, index);
    }

    @GetMapping("/{id}/view")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public ResponseEntity<byte[]> viewContratImage(
            @PathVariable String id,
            @RequestParam("index") int index) {
        return contratService.viewContratImage(id, index);
    }

    @DeleteMapping("/{id}/images/{index}")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<ContratDTO> deleteContratImage(
            @PathVariable String id,
            @PathVariable int index) {
        String currentUser = getCurrentUser();
        ContratDTO contrat = contratService.deleteContratImage(id, index, currentUser);
        return ResponseEntity.ok(contrat);
    }

    // ==========================================
    // CONSULTATION - TOUS LES CONTRATS
    // ==========================================
    @GetMapping
    @PreAuthorize("hasAuthority('CONTRACT_VIEW_ALL')")
    public ResponseEntity<List<ContratDTO>> getAllContrats() {
        return ResponseEntity.ok(contratService.getAllContrats());
    }

    // ==========================================
    // CONSULTATION - PAR ID
    // ==========================================
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public ResponseEntity<ContratDTO> getContratById(@PathVariable String id) {
        ContratDTO contrat = contratService.getContratById(id);
        if (!hasAuthority("CONTRACT_VIEW_ALL") && !contrat.getEmployeeId().equals(getCurrentEmployeeId())) {
            throw new AccessDeniedException("You can only view your own contracts");
        }
        return ResponseEntity.ok(contrat);
    }

    // ==========================================
    // CONSULTATION - PAR EMPLOYE
    // ==========================================
    @GetMapping("/employe/{employeeId}")
    public ResponseEntity<List<ContratDTO>> getContratsByEmployee(@PathVariable String employeeId) {
        log.info("Recuperation des contrats pour l'employe ID: {}", employeeId);

        if (!hasAuthority("CONTRACT_VIEW")) {
            log.warn("Acces refuse : l'utilisateur n'a pas l'autorite CONTRACT_VIEW");
            throw new AccessDeniedException("Vous n'avez pas la permission de consulter les contrats.");
        }

        if (!hasAuthority("CONTRACT_VIEW_ALL") && !employeeId.equals(getCurrentEmployeeId())) {
            log.warn("Acces refuse : l'utilisateur tente de consulter les contrats d'un autre employe");
            throw new AccessDeniedException("Vous ne pouvez consulter que vos propres contrats.");
        }

        return ResponseEntity.ok(contratService.getContratsByEmployee(employeeId));
    }

    // ==========================================
    // CONSULTATION - CONTRAT ACTIF D'UN EMPLOYE
    // ==========================================
    @GetMapping("/employe/{employeeId}/actif")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW_ALL')")
    public ResponseEntity<ContratDTO> getActiveContratByEmployee(@PathVariable String employeeId) {
        return ResponseEntity.ok(contratService.getActiveContratByEmployee(employeeId));
    }

    // ==========================================
    // CONSULTATION - PAR STATUT
    // ==========================================
    @GetMapping("/statut/{statut}")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW_ALL')")
    public ResponseEntity<List<ContratDTO>> getContratsByStatut(@PathVariable String statut) {
        return ResponseEntity.ok(contratService.getContratsByStatut(statut));
    }

    // ==========================================
    // CONSULTATION - PAR TYPE
    // ==========================================
    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW_ALL')")
    public ResponseEntity<List<ContratDTO>> getContratsByType(@PathVariable String type) {
        return ResponseEntity.ok(contratService.getContratsByType(type));
    }

    // ==========================================
    // CONSULTATION - CONTRATS ACTIFS
    // ==========================================
    @GetMapping("/actifs")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW_ALL')")
    public ResponseEntity<List<ContratDTO>> getActiveContracts() {
        return ResponseEntity.ok(contratService.getActiveContracts());
    }

    // ==========================================
    // CONSULTATION - CONTRATS EXPIRES
    // ==========================================
    @GetMapping("/expires")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW_ALL')")
    public ResponseEntity<List<ContratDTO>> getExpiredContracts() {
        return ResponseEntity.ok(contratService.getExpiredContracts());
    }

    // ==========================================
    // CONSULTATION - CONTRATS PROCHES DE L'EXPIRATION
    // ==========================================
    @GetMapping("/expirant")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW_ALL')")
    public ResponseEntity<List<ContratDTO>> getContractsExpiringSoon(@RequestParam(defaultValue = "14") int days) {
        log.info("Recuperation des contrats expirant dans {} jours", days);
        List<ContratDTO> contracts = contratService.getContractsExpiringSoon(days);
        log.info("{} contrats trouves", contracts.size());
        return ResponseEntity.ok(contracts);
    }

    // ==========================================
    // CONSULTATION - CONTRATS RECENTS
    // ==========================================
    @GetMapping("/recents")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW_ALL')")
    public ResponseEntity<List<ContratDTO>> getRecentContracts() {
        return ResponseEntity.ok(contratService.getRecentContracts());
    }

    // ==========================================
    // CONSULTATION - RECHERCHE
    // ==========================================
    @GetMapping("/search")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW_ALL')")
    public ResponseEntity<List<ContratDTO>> searchContrats(@RequestParam String term) {
        return ResponseEntity.ok(contratService.searchContrats(term));
    }

    // ==========================================
    // CONSULTATION - STATISTIQUES
    // ==========================================
    @GetMapping("/statistiques")
    @PreAuthorize("hasAuthority('REPORT_VIEW')")
    public ResponseEntity<StatistiquesContratDTO> getStatistiques() {
        return ResponseEntity.ok(contratService.getStatistiques());
    }

    // ==========================================
    // DECLENCHER MANUELLEMENT LA VERIFICATION DES CONTRATS
    // ==========================================
    @PostMapping("/check-expiring")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW_ALL')")
    public ResponseEntity<Map<String, Object>> checkExpiringContracts() {
        log.info("Declenchement manuel de la verification des contrats");
        Map<String, Object> result = contratExpirationScheduler.manualCheck();
        return ResponseEntity.ok(result);
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================
    private String getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            return authentication.getName();
        }
        return "SYSTEM";
    }

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        log.debug("Autorites disponibles pour l'utilisateur : {}", authorities);
        boolean has = authorities.contains(authority);
        log.debug("Recherche de l'autorite {} : {}", authority, has);
        return has;
    }

    private String getCurrentEmployeeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Not authenticated");
        }
        String username = authentication.getName();
        User user = userService.findByEmail(username);
        if (user == null) {
            throw new AccessDeniedException("User not found");
        }
        Employee employee = employeeService.findByUserId(user.getId());
        if (employee == null) {
            throw new AccessDeniedException("Employee not found for user");
        }
        return employee.getId();
    }
}
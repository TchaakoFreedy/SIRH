package com.fric.sirh.controller;

import com.fric.sirh.dto.*;
import com.fric.sirh.dto.history.EmployeeHistoryEvent;
import com.fric.sirh.dto.history.EmployeeHistoryResponse;
import com.fric.sirh.model.Documents;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.notification.security.SecurityUtils;
import com.fric.sirh.security.CustomUserDetails;
import com.fric.sirh.service.*;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employees")

public class EmployeeController {

    private static final Logger log = LoggerFactory.getLogger(EmployeeController.class);

    private final EmployeeService employeeService;
    private final ContratService contratService;
    private final DocumentService documentService;
    private final UserService userService;
    private final SecurityUtils securityUtils;
    private final EmployeeHistoryService employeeHistoryService;

    public EmployeeController(EmployeeService employeeService,
                              ContratService contratService,
                              DocumentService documentService,
                              UserService userService,
                              SecurityUtils securityUtils,
                              EmployeeHistoryService employeeHistoryService) {
        this.employeeService = employeeService;
        this.contratService = contratService;
        this.documentService = documentService;
        this.userService = userService;
        this.securityUtils = securityUtils;
        this.employeeHistoryService = employeeHistoryService;
    }

    // ==========================================
    // UTILITY METHODS FOR AUTHENTICATION
    // ==========================================

    private String getCurrentUserId(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return ((CustomUserDetails) principal).getId();
        }
        String identifier = authentication.getName();
        log.warn("Le principal n'est pas CustomUserDetails, tentative de recherche par identifiant: {}", identifier);

        try {
            User user = userService.findByEmail(identifier.toLowerCase());
            if (user != null) {
                log.debug("Utilisateur trouve par email: {}", user.getId());
                return user.getId();
            }
        } catch (Exception e) {
            log.debug("Recherche par email echouee: {}", e.getMessage());
        }

        if (identifier.matches("^[a-fA-F0-9]{24}$")) {
            try {
                User user = userService.findById(identifier);
                if (user != null) {
                    log.debug("Utilisateur trouve par ID: {}", user.getId());
                    return user.getId();
                }
            } catch (Exception e) {
                log.debug("Recherche par ID echouee: {}", e.getMessage());
            }
        }

        log.warn("Impossible de trouver l'utilisateur avec l'identifiant: {}, retour de l'identifiant brut", identifier);
        return identifier;
    }

    private Employee getAuthenticatedEmployee(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        Employee employee = employeeService.findByUserId(userId);
        if (employee == null) {
            log.warn("Aucun employe trouve pour l'utilisateur ID: {}", userId);
        }
        return employee;
    }

    private boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role) ||
                        a.getAuthority().equals("ROLE_" + role));
    }

    private String safeToString(Object value) {
        return value != null ? value.toString() : null;
    }

    private LocalDate safeParseDate(Object value) {
        if (value == null) {
            return null;
        }
        try {
            if (value instanceof String) {
                return LocalDate.parse((String) value);
            }
            log.warn("Date value is not a String: {}", value.getClass().getName());
            return null;
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", value);
            return null;
        }
    }

    // ==========================================
    // GET ALL EMPLOYEES
    // ==========================================
    @GetMapping
    @PreAuthorize("hasAuthority('EMPLOYEE_VIEW_ALL') or hasAuthority('EMPLOYEE_VIEW')")
    public ResponseEntity<List<Employee>> getAllEmployees(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("Recuperation des employes pour l'utilisateur {}", userId);

        try {
            boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
            boolean isDirection = hasRole(authentication, "DIRECTION");
            boolean isTopManager = hasRole(authentication, "TOP_MANAGER");
            boolean isEmployee = hasRole(authentication, "EMPLOYEE");

            List<Employee> employees;

            if (isRH || isTopManager) {
                employees = employeeService.getAll();
                log.info("Admin/RH/TOP_MANAGER - {} employes retournes", employees.size());
            } else if (isDirection) {
                Employee directionEmployee = getAuthenticatedEmployee(authentication);
                if (directionEmployee == null || directionEmployee.getDepartementId() == null) {
                    log.warn("Direction sans employe ou departement, retourne liste vide");
                    return ResponseEntity.ok(List.of());
                }
                employees = employeeService.getEmployeesBySameCompany(directionEmployee.getDepartementId());
                log.info("Direction - {} employes retournes de son entreprise", employees.size());
            } else if (isEmployee) {
                Employee employee = getAuthenticatedEmployee(authentication);
                if (employee == null) {
                    log.warn("Employee sans employe associe, retourne liste vide");
                    return ResponseEntity.ok(List.of());
                }
                employees = List.of(employee);
                log.info("Employee - 1 employe retourne (lui-meme)");
            } else {
                Employee manager = getAuthenticatedEmployee(authentication);
                if (manager == null || manager.getDepartementId() == null) {
                    log.warn("Manager sans departement, retourne liste vide");
                    return ResponseEntity.ok(List.of());
                }
                employees = employeeService.getEmployeesByDepartement(manager.getDepartementId());
                log.info("Manager - {} employes retournes du departement {}",
                        employees.size(), manager.getDepartementId());
            }

            return ResponseEntity.ok(employees);

        } catch (Exception e) {
            log.error("Erreur lors de la recuperation des employes: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // GET EMPLOYEE BY ID
    // ==========================================
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> getEmployeeById(@PathVariable("id") String id, Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("Recuperation de l'employe ID: {}", id);

        try {
            Employee employee = employeeService.getById(id);
            if (employee == null) {
                log.warn("Employe non trouve avec l'ID: {}", id);
                return ResponseEntity.notFound().build();
            }

            boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
            boolean isTopManager = hasRole(authentication, "TOP_MANAGER");
            boolean isDirection = hasRole(authentication, "DIRECTION");
            boolean isEmployee = hasRole(authentication, "EMPLOYEE");

            if (isRH || isTopManager) {
                log.info("RH/TOP_MANAGER - Acces autorise a l'employe: {}", id);
                return ResponseEntity.ok(employee);
            }

            if (isDirection) {
                Employee directionEmployee = getAuthenticatedEmployee(authentication);
                if (directionEmployee == null || directionEmployee.getDepartementId() == null) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
                String employeeDeptId = employee.getDepartementId();
                if (employeeDeptId == null ||
                        !employeeService.areEmployeesInSameCompany(directionEmployee.getDepartementId(), employeeDeptId)) {
                    log.warn("Direction - Acces refuse a l'employe d'une autre entreprise: {}", id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
                log.info("Direction - Acces autorise a l'employe: {}", id);
                return ResponseEntity.ok(employee);
            }

            if (isEmployee) {
                Employee currentEmployee = getAuthenticatedEmployee(authentication);
                if (currentEmployee == null || !currentEmployee.getId().equals(id)) {
                    log.warn("Employee - Acces refuse a l'employe {} (n'est pas lui-meme)", id);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
                log.info("Employee - Acces autorise a son propre profil");
                return ResponseEntity.ok(employee);
            }

            Employee manager = getAuthenticatedEmployee(authentication);
            if (manager == null || manager.getDepartementId() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (employee.getDepartementId() == null ||
                    !manager.getDepartementId().equals(employee.getDepartementId())) {
                log.warn("Manager - Acces refuse a l'employe d'un autre departement: {}", id);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            log.info("Employe {} trouve: {} {}", id, employee.getPrenom(), employee.getNom());
            return ResponseEntity.ok(employee);

        } catch (Exception e) {
            log.error("Erreur lors de la recuperation: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // GET EMPLOYEE BY USER ID
    // ==========================================
    @GetMapping("/user/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> getEmployeeByUserId(@PathVariable("userId") String userId, Authentication authentication) {
        log.info("Recuperation de l'employe par userId: {}", userId);

        try {
            String currentUserId = getCurrentUserId(authentication);
            boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
            boolean isTopManager = hasRole(authentication, "TOP_MANAGER");
            boolean isEmployee = hasRole(authentication, "EMPLOYEE");

            Employee employee = employeeService.findByUserId(userId);
            if (employee == null) {
                log.warn("Aucun employe trouve avec userId: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            if (isEmployee && !currentUserId.equals(userId)) {
                log.warn("Employee - Acces refuse a l'utilisateur {}", userId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (!isRH && !isTopManager && !isEmployee && !currentUserId.equals(userId)) {
                Employee manager = getAuthenticatedEmployee(authentication);
                if (manager == null || manager.getDepartementId() == null ||
                        !manager.getDepartementId().equals(employee.getDepartementId())) {
                    log.warn("Acces refuse a l'employe avec userId: {}", userId);
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            log.info("Employe trouve: {} {}", employee.getPrenom(), employee.getNom());
            return ResponseEntity.ok(employee);

        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // GET CURRENT EMPLOYEE
    // ==========================================
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> getCurrentEmployeeEndpoint(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("Recuperation de l'employe connecte: {}", userId);

        try {
            Employee employee = employeeService.findByUserId(userId);
            if (employee == null) {
                log.warn("Aucun employe trouve pour l'utilisateur connecte: {}", userId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }
            log.info("Employe connecte: {} {}", employee.getPrenom(), employee.getNom());
            return ResponseEntity.ok(employee);
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // GET PROFILE OF CURRENT USER
    // ==========================================
    @GetMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> getCurrentUserProfile(Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("Recuperation du profil de l'utilisateur: {}", userId);

        Employee employee = employeeService.findByUserId(userId);
        if (employee == null) {
            log.warn("Aucun employe trouve pour l'utilisateur: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        return ResponseEntity.ok(employee);
    }

    // ==========================================
    // UPDATE PROFILE OF CURRENT USER
    // ==========================================
    @PatchMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> updateCurrentUserProfile(
            @RequestBody EmployeeSelfUpdateRequest updateData,
            Authentication authentication) {

        String userId = getCurrentUserId(authentication);
        log.info("Mise a jour du profil pour l'utilisateur: {}", userId);

        Employee employee = employeeService.findByUserId(userId);
        if (employee == null) {
            log.warn("Aucun employe trouve pour l'utilisateur: {}", userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        // Utiliser la méthode updateSelfProfile du service
        Employee updatedEmployee = employeeService.updateSelfProfile(employee.getId(), updateData);
        log.info("Profil mis a jour pour: {}", userId);

        return ResponseEntity.ok(updatedEmployee);
    }

    // ==========================================
    // SEARCH EMPLOYEES
    // ==========================================
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Employee>> searchEmployees(
            @RequestParam("q") String query,
            Authentication authentication) {
        String userId = getCurrentUserId(authentication);
        log.info("Recherche d'employes: {}", query);

        try {
            boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
            boolean isTopManager = hasRole(authentication, "TOP_MANAGER");
            boolean isDirection = hasRole(authentication, "DIRECTION");
            boolean isEmployee = hasRole(authentication, "EMPLOYEE");

            List<Employee> results;

            if (isRH || isTopManager) {
                results = employeeService.search(query);
                log.info("Admin/TOP_MANAGER - {} employes trouves", results.size());
            } else if (isDirection) {
                Employee directionEmployee = getAuthenticatedEmployee(authentication);
                if (directionEmployee == null || directionEmployee.getDepartementId() == null) {
                    return ResponseEntity.ok(List.of());
                }
                results = employeeService.searchInCompany(query, directionEmployee.getDepartementId());
                log.info("Direction - {} employes trouves dans son entreprise", results.size());
            } else if (isEmployee) {
                Employee employee = getAuthenticatedEmployee(authentication);
                if (employee == null) {
                    return ResponseEntity.ok(List.of());
                }
                String queryLower = query.toLowerCase();
                boolean matches = (employee.getNom() != null && employee.getNom().toLowerCase().contains(queryLower)) ||
                        (employee.getPrenom() != null && employee.getPrenom().toLowerCase().contains(queryLower)) ||
                        (employee.getMatriculeInterne() != null && employee.getMatriculeInterne().toLowerCase().contains(queryLower));
                results = matches ? List.of(employee) : List.of();
                log.info("Employee - {} employe trouve", results.size());
            } else {
                Employee manager = getAuthenticatedEmployee(authentication);
                if (manager == null || manager.getDepartementId() == null) {
                    return ResponseEntity.ok(List.of());
                }
                List<Employee> allInDept = employeeService.getEmployeesByDepartement(manager.getDepartementId());
                String queryLower = query.toLowerCase();
                results = allInDept.stream()
                        .filter(e ->
                                (e.getNom() != null && e.getNom().toLowerCase().contains(queryLower)) ||
                                        (e.getPrenom() != null && e.getPrenom().toLowerCase().contains(queryLower)) ||
                                        (e.getMatriculeInterne() != null && e.getMatriculeInterne().toLowerCase().contains(queryLower))
                        )
                        .toList();
                log.info("Manager - {} employes trouves", results.size());
            }

            return ResponseEntity.ok(results);

        } catch (Exception e) {
            log.error("Erreur lors de la recherche: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // GET EMPLOYEES BY DEPARTEMENT
    // ==========================================
    @GetMapping("/departement/{departementId}")
    @PreAuthorize("hasAuthority('LEAVE_VIEW_TEAM') or hasAuthority('LEAVE_VIEW_ALL')")
    public ResponseEntity<List<Employee>> getEmployeesByDepartement(
            @PathVariable String departementId,
            Authentication authentication) {
        String userId = getCurrentUserId(authentication);

        boolean isAdmin = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
        boolean isTopManager = hasRole(authentication, "TOP_MANAGER");
        boolean isEmployee = hasRole(authentication, "EMPLOYEE");

        if (isEmployee) {
            log.warn("Employee - Acces refuse a la liste des employes du departement");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!isAdmin && !isTopManager) {
            Employee manager = getAuthenticatedEmployee(authentication);
            if (manager == null || manager.getDepartementId() == null ||
                    !manager.getDepartementId().equals(departementId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        return ResponseEntity.ok(employeeService.getEmployeesByDepartement(departementId));
    }

    // ==========================================
    // GET EMPLOYEE CONTRACTS
    // ==========================================
    @GetMapping("/{id}/contracts")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public ResponseEntity<List<ContratDTO>> getEmployeeContracts(@PathVariable("id") String id) {
        return ResponseEntity.ok(contratService.getContratsByEmployee(id));
    }

    // ==========================================
    // GET EMPLOYEE DOCUMENTS
    // ==========================================
    @GetMapping("/{id}/documents")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Documents>> getEmployeeDocuments(@PathVariable("id") String id) {
        try {
            return ResponseEntity.ok(documentService.getDocumentsByEmployee(id));
        } catch (Exception e) {
            log.error("Erreur: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // CREATE EMPLOYEE
    // ==========================================
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('USER_CREATE')")
    public ResponseEntity<Employee> createFullEmployee(
            @RequestPart("request") EmployeeFullCreationRequest request,
            @RequestPart(value = "cniFiles", required = false) List<MultipartFile> cniFiles,
            @RequestPart(value = "contratFiles", required = false) List<MultipartFile> contratFiles,
            @RequestPart(value = "diplomeFiles", required = false) List<MultipartFile> diplomeFiles,
            @RequestPart(value = "photoFiles", required = false) List<MultipartFile> photoFiles,
            @RequestPart(value = "certificatFiles", required = false) List<MultipartFile> certificatFiles,
            Authentication authentication) {

        log.info("Creation employe - Role recu: {}", request.getRoleId());
        log.info("Creation employe - Email: {}", request.getEmail());
        log.info("Creation employe - Nom: {}", request.getNom());

        String creatorId = getCurrentUserId(authentication);

        List<MultipartFile> allFiles = new ArrayList<>();
        if (cniFiles != null) allFiles.addAll(cniFiles);
        if (contratFiles != null) allFiles.addAll(contratFiles);
        if (diplomeFiles != null) allFiles.addAll(diplomeFiles);
        if (photoFiles != null) allFiles.addAll(photoFiles);
        if (certificatFiles != null) allFiles.addAll(certificatFiles);

        Employee createdEmployee = employeeService.createEmployeeWithAccount(request, allFiles, creatorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEmployee);
    }

    // ==========================================
    // UPDATE EMPLOYEE PROFILE (SELF)
    // ==========================================
    @PatchMapping("/{id}/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Employee> updateEmployeeProfile(
            @PathVariable("id") String id,
            @RequestBody EmployeeSelfUpdateRequest updateData,
            Authentication authentication) {

        String userId = getCurrentUserId(authentication);
        log.info("Mise a jour du profil pour userId: {}", userId);

        Employee employee = employeeService.getById(id);

        if (employee.getUser() == null || !employee.getUser().getId().equals(userId)) {
            log.warn("Acces refuse: {} tente de modifier le profil de {}", userId, id);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Employee updatedEmployee = employeeService.updateSelfProfile(id, updateData);
        log.info("Profil mis a jour pour: {}", userId);

        return ResponseEntity.ok(updatedEmployee);
    }

    // ==========================================
    // UPDATE EMPLOYEE BY ADMIN
    // ==========================================
    @PatchMapping("/{id}/admin")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<Employee> updateEmployeeByAdmin(
            @PathVariable("id") String id,
            @RequestBody EmployeeAdminUpdateRequest updateData,
            Authentication authentication) {
        String adminId = getCurrentUserId(authentication);
        userService.getById(adminId);
        return ResponseEntity.ok(employeeService.updateByAdmin(id, updateData, adminId));
    }

    // ==========================================
    // SUSPEND EMPLOYEE
    // ==========================================
    @PostMapping("/{id}/suspendre")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<Employee> suspendEmployee(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(employeeService.suspendre(id, getCurrentUserId(auth)));
    }

    // ==========================================
    // REACTIVATE EMPLOYEE
    // ==========================================
    @PostMapping("/{id}/reactiver")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<Employee> reactivateEmployee(@PathVariable String id, Authentication auth) {
        return ResponseEntity.ok(employeeService.reactiver(id, getCurrentUserId(auth)));
    }

    // ==========================================
    // DELETE EMPLOYEE
    // ==========================================
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_DELETE')")
    public ResponseEntity<Void> deleteEmployee(@PathVariable("id") String id, Authentication authentication) {
        employeeService.deleteEmployee(id, getCurrentUserId(authentication));
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // GET EMPLOYEE COUNT
    // ==========================================
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Long> getEmployeeCount() {
        return ResponseEntity.ok((long) employeeService.getAll().size());
    }

    // ==========================================
    // UPDATE EMPLOYEE (PATCH generique)
    // ==========================================
    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('USER_UPDATE')")
    public ResponseEntity<Employee> updateEmployee(
            @PathVariable("id") String id,
            @RequestBody Map<String, Object> updates,
            Authentication authentication) {

        String adminId = getCurrentUserId(authentication);
        log.info("Mise a jour de l'employe {} par {}", id, adminId);

        EmployeeAdminUpdateRequest request = new EmployeeAdminUpdateRequest();

        try {
            if (updates.containsKey("nom")) {
                request.setNom(safeToString(updates.get("nom")));
            }
            if (updates.containsKey("prenom")) {
                request.setPrenom(safeToString(updates.get("prenom")));
            }
            if (updates.containsKey("matricule_CNPS")) {
                request.setMatricule_CNPS(safeToString(updates.get("matricule_CNPS")));
            }
            if (updates.containsKey("sexe")) {
                request.setSexe(safeToString(updates.get("sexe")));
            }
            if (updates.containsKey("telephone")) {
                request.setTelephone(safeToString(updates.get("telephone")));
            }
            if (updates.containsKey("numeroContactUrgence")) {
                request.setNumeroContactUrgence(safeToString(updates.get("numeroContactUrgence")));
            }
            if (updates.containsKey("addresse")) {
                request.setAddresse(safeToString(updates.get("addresse")));
            }
            if (updates.containsKey("posteId")) {
                request.setPosteId(safeToString(updates.get("posteId")));
                log.debug("posteId set to: {}", request.getPosteId());
            }
            if (updates.containsKey("departementId")) {
                request.setDepartementId(safeToString(updates.get("departementId")));
                log.debug("departementId set to: {}", request.getDepartementId());
            }
            if (updates.containsKey("statut")) {
                request.setStatut(safeToString(updates.get("statut")));
                log.debug("statut set to: {}", request.getStatut());
            }
            if (updates.containsKey("date_naissance")) {
                request.setDate_naissance(safeParseDate(updates.get("date_naissance")));
            }
            if (updates.containsKey("date_embauche")) {
                request.setDate_embauche(safeParseDate(updates.get("date_embauche")));
            }

            Employee updatedEmployee = employeeService.updateByAdmin(id, request, adminId);
            log.info("Employe {} mis a jour avec succes", id);

            return ResponseEntity.ok(updatedEmployee);

        } catch (ClassCastException e) {
            log.error("Erreur de conversion de type: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Erreur lors de la mise a jour de l'employe: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==========================================
    // GET EMPLOYEE HISTORY (JSON)
    // ==========================================
    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EmployeeHistoryResponse> getEmployeeHistory(
            @PathVariable("id") String id,
            Authentication authentication) {

        Employee employee = employeeService.getById(id);
        if (employee == null) {
            return ResponseEntity.notFound().build();
        }

        String userId = getCurrentUserId(authentication);
        boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
        boolean isTopManager = hasRole(authentication, "TOP_MANAGER");
        boolean isDirection = hasRole(authentication, "DIRECTION");
        boolean isEmployeeRole = hasRole(authentication, "EMPLOYEE");

        if (isRH || isTopManager) {
            // accès total
        } else if (isDirection) {
            Employee directionEmployee = getAuthenticatedEmployee(authentication);
            if (directionEmployee == null || directionEmployee.getDepartementId() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            String employeeDeptId = employee.getDepartementId();
            if (employeeDeptId == null ||
                    !employeeService.areEmployeesInSameCompany(directionEmployee.getDepartementId(), employeeDeptId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else if (isEmployeeRole) {
            Employee currentEmployee = getAuthenticatedEmployee(authentication);
            if (currentEmployee == null || !currentEmployee.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            Employee manager = getAuthenticatedEmployee(authentication);
            if (manager == null || manager.getDepartementId() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (employee.getDepartementId() == null ||
                    !manager.getDepartementId().equals(employee.getDepartementId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        EmployeeHistoryResponse history = employeeHistoryService.getEmployeeHistory(id);
        return ResponseEntity.ok(history);
    }

    // ==========================================
    // DOWNLOAD EMPLOYEE HISTORY (CSV ou PDF)
    // ==========================================
    @GetMapping("/{id}/history/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadEmployeeHistory(
            @PathVariable("id") String id,
            @RequestParam(value = "format", defaultValue = "csv") String format,
            Authentication authentication) throws IOException, DocumentException {

        Employee employee = employeeService.getById(id);
        if (employee == null) {
            return ResponseEntity.notFound().build();
        }

        String userId = getCurrentUserId(authentication);
        boolean isRH = hasRole(authentication, "RH") || hasRole(authentication, "SUPER_ADMIN");
        boolean isTopManager = hasRole(authentication, "TOP_MANAGER");
        boolean isDirection = hasRole(authentication, "DIRECTION");
        boolean isEmployeeRole = hasRole(authentication, "EMPLOYEE");

        if (isRH || isTopManager) {
            // accès total
        } else if (isDirection) {
            Employee directionEmployee = getAuthenticatedEmployee(authentication);
            if (directionEmployee == null || directionEmployee.getDepartementId() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            String employeeDeptId = employee.getDepartementId();
            if (employeeDeptId == null ||
                    !employeeService.areEmployeesInSameCompany(directionEmployee.getDepartementId(), employeeDeptId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else if (isEmployeeRole) {
            Employee currentEmployee = getAuthenticatedEmployee(authentication);
            if (currentEmployee == null || !currentEmployee.getId().equals(id)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        } else {
            Employee manager = getAuthenticatedEmployee(authentication);
            if (manager == null || manager.getDepartementId() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (employee.getDepartementId() == null ||
                    !manager.getDepartementId().equals(employee.getDepartementId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
        }

        EmployeeHistoryResponse history = employeeHistoryService.getEmployeeHistory(id);

        byte[] fileContent;
        String fileName;
        String contentType;

        if ("pdf".equalsIgnoreCase(format)) {
            fileContent = generatePdf(history);
            fileName = "historique_employe_" + history.getEmployeeId() + ".pdf";
            contentType = MediaType.APPLICATION_PDF_VALUE;
        } else {
            fileContent = generateCsv(history);
            fileName = "historique_employe_" + history.getEmployeeId() + ".csv";
            contentType = "text/csv";
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(contentType));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");

        return ResponseEntity.ok()
                .headers(headers)
                .body(fileContent);
    }

    // ==========================================
    // PRIVATE METHODS FOR DOWNLOAD
    // ==========================================

    private byte[] generateCsv(EmployeeHistoryResponse history) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        writer.println("Date,Type,Description,Details");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (EmployeeHistoryEvent event : history.getEvents()) {
            String dateStr = event.getDate() != null ? event.getDate().format(formatter) : "";
            String type = event.getType() != null ? event.getType() : "";
            String desc = event.getDescription() != null ? event.getDescription() : "";
            String details = event.getDetails() != null ? event.getDetails().toString() : "";
            details = details.replace(",", ";");
            writer.printf("%s,%s,%s,%s%n", dateStr, type, desc, details);
        }
        writer.close();
        return stringWriter.toString().getBytes();
    }

    private byte[] generatePdf(EmployeeHistoryResponse history) throws DocumentException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document document = new Document();
        PdfWriter.getInstance(document, baos);
        document.open();

        document.add(new Paragraph("Historique de l'employe : " + history.getEmployeeName()));
        document.add(new Paragraph("ID : " + history.getEmployeeId()));
        document.add(new Paragraph(" "));

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.addCell("Date");
        table.addCell("Type");
        table.addCell("Description");
        table.addCell("Details");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        for (EmployeeHistoryEvent event : history.getEvents()) {
            String dateStr = event.getDate() != null ? event.getDate().format(formatter) : "";
            table.addCell(dateStr);
            table.addCell(event.getType() != null ? event.getType() : "");
            table.addCell(event.getDescription() != null ? event.getDescription() : "");
            table.addCell(event.getDetails() != null ? event.getDetails().toString() : "");
        }

        document.add(table);
        document.close();
        return baos.toByteArray();
    }
}
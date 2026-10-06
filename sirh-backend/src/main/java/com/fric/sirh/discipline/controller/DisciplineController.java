package com.fric.sirh.discipline.controller;

import com.fric.sirh.discipline.dto.DashboardSanctionStats;
import com.fric.sirh.discipline.dto.DemandeExplicationDTO;
import com.fric.sirh.discipline.dto.DisciplineFilterDTO;
import com.fric.sirh.discipline.dto.ReponseExplicationDTO;
import com.fric.sirh.discipline.dto.SanctionDTO;
import com.fric.sirh.discipline.service.DisciplineService;
import com.fric.sirh.discipline.service.SanctionService;
import com.fric.sirh.notification.security.SecurityUtils;
import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.EmployeeRepository;
import com.fric.sirh.repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/api/discipline")
@RequiredArgsConstructor
@CrossOrigin("*")
public class DisciplineController {

    private final DisciplineService disciplineService;
    private final SanctionService sanctionService;
    private final SecurityUtils securityUtils;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    // ==================== DEMANDES D'EXPLICATION ====================

    @GetMapping("/explanations")
    @PreAuthorize("hasAuthority('EXPLANATION_REQUEST_VIEW')")
    public ResponseEntity<Page<DemandeExplicationDTO>> getAllExplanations(
            @Valid DisciplineFilterDTO filter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(disciplineService.getAllDemandes(filter, pageable));
    }

    @GetMapping("/explanations/employee/{employeeId}")
    @PreAuthorize("hasAuthority('EXPLANATION_REQUEST_VIEW_OWN')")
    public ResponseEntity<Page<DemandeExplicationDTO>> getExplanationsByEmployee(
            @PathVariable String employeeId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(disciplineService.getDemandesByEmployee(employeeId, pageable));
    }

    @GetMapping("/explanations/{id}")
    @PreAuthorize("hasAnyAuthority('EXPLANATION_REQUEST_VIEW', 'EXPLANATION_REQUEST_VIEW_OWN', 'EXPLANATION_REQUEST_RESPOND')")
    public ResponseEntity<DemandeExplicationDTO> getExplanationById(@PathVariable String id) {
        return ResponseEntity.ok(disciplineService.getDemandeById(id));
    }

    @PostMapping("/explanations")
    @PreAuthorize("hasAuthority('EXPLANATION_REQUEST_CREATE')")
    public ResponseEntity<DemandeExplicationDTO> createExplanation(@Valid @RequestBody DemandeExplicationDTO dto) {
        String userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(disciplineService.createDemande(dto, userId));
    }

    @PutMapping("/explanations/{id}")
    @PreAuthorize("hasAuthority('EXPLANATION_REQUEST_UPDATE')")
    public ResponseEntity<DemandeExplicationDTO> updateExplanation(
            @PathVariable String id,
            @Valid @RequestBody DemandeExplicationDTO dto) {
        String userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(disciplineService.updateDemande(id, dto, userId));
    }

    @PostMapping("/explanations/{id}/reply")
    @PreAuthorize("hasAuthority('EXPLANATION_REQUEST_RESPOND')")
    public ResponseEntity<DemandeExplicationDTO> replyToExplanation(
            @PathVariable String id,
            @Valid @RequestBody ReponseExplicationDTO dto) {
        String userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(disciplineService.repondreDemande(id, dto, userId));
    }

    @PostMapping("/explanations/{id}/mark-replied")
    @PreAuthorize("hasAuthority('EXPLANATION_REQUEST_RESPOND')")
    public ResponseEntity<DemandeExplicationDTO> markAsReplied(@PathVariable String id) {
        String userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(disciplineService.marquerCommeRepondue(id, userId));
    }

    @PostMapping("/explanations/{id}/validate")
    @PreAuthorize("hasAuthority('HR_REQUEST_APPROVE')")
    public ResponseEntity<DemandeExplicationDTO> validateResponse(@PathVariable String id) {
        String userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(disciplineService.validerReponse(id, userId));
    }

    @PostMapping("/explanations/{id}/reject")
    @PreAuthorize("hasAuthority('HR_REQUEST_REJECT')")
    public ResponseEntity<DemandeExplicationDTO> rejectResponse(@PathVariable String id) {
        String userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(disciplineService.rejeterReponse(id, userId));
    }

    @PostMapping("/explanations/{id}/cancel")
    @PreAuthorize("hasAuthority('EXPLANATION_REQUEST_DELETE')")
    public ResponseEntity<Void> cancelExplanation(@PathVariable String id) {
        String userId = securityUtils.getCurrentUserId();
        disciplineService.annulerDemande(id, userId);
        return ResponseEntity.ok().build();
    }

    // ==================== SANCTIONS ====================

    @GetMapping("/sanctions")
    @PreAuthorize("hasAnyAuthority('SANCTION_VIEW', 'SANCTION_VIEW_ALL')")
    public ResponseEntity<Page<SanctionDTO>> getAllSanctions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            String userId = securityUtils.getCurrentUserId();
            log.info("Recuperation des sanctions pour l'utilisateur: {}", userId);

            Page<SanctionDTO> sanctions = sanctionService.getSanctionsByRole(userId, pageable);
            log.info("{} sanctions trouvees", sanctions.getTotalElements());

            return ResponseEntity.ok(sanctions);
        } catch (Exception e) {
            log.error("Erreur lors de la recuperation des sanctions: {}", e.getMessage(), e);
            return ResponseEntity.ok(new PageImpl<>(Collections.emptyList(), pageable, 0));
        }
    }

    // ============================================
    // NOUVEAU : Dashboard Manager
    // ============================================

    @GetMapping("/sanctions/dashboard/manager")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<Page<SanctionDTO>> getDashboardManagerSanctions(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = securityUtils.getCurrentUserId();
        log.info("Dashboard Manager - Recuperation des sanctions pour l'utilisateur: {}", userId);
        Page<SanctionDTO> sanctions = sanctionService.getDashboardManagerSanctions(userId, pageable);
        return ResponseEntity.ok(sanctions);
    }

    // ============================================
    // NOUVEAU : Dashboard Direction
    // ============================================

    @GetMapping("/sanctions/dashboard/direction")
    @PreAuthorize("hasRole('DIRECTION')")
    public ResponseEntity<Page<SanctionDTO>> getDashboardDirectionSanctions(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = securityUtils.getCurrentUserId();
        log.info("Dashboard Direction - Recuperation des sanctions pour l'utilisateur: {}", userId);
        Page<SanctionDTO> sanctions = sanctionService.getDashboardDirectionSanctions(userId, pageable);
        return ResponseEntity.ok(sanctions);
    }

    // ============================================
    // NOUVEAU : Statistiques du dashboard
    // ============================================

    @GetMapping("/sanctions/dashboard/stats")
    @PreAuthorize("hasAnyRole('MANAGER', 'DIRECTION')")
    public ResponseEntity<DashboardSanctionStats> getDashboardStats() {
        String userId = securityUtils.getCurrentUserId();
        log.info("Dashboard Stats - Recuperation des statistiques pour l'utilisateur: {}", userId);
        DashboardSanctionStats stats = sanctionService.getDashboardStats(userId);
        return ResponseEntity.ok(stats);
    }

    // ============================================
    // METHODES EXISTANTES (SANCTIONS)
    // ============================================

    @GetMapping("/sanctions/{id}")
    @PreAuthorize("hasAnyAuthority('SANCTION_VIEW', 'SANCTION_VIEW_OWN')")
    public ResponseEntity<SanctionDTO> getSanctionById(@PathVariable String id) {
        String userId = securityUtils.getCurrentUserId();
        log.info("Recuperation de la sanction {} par l'utilisateur {}", id, userId);

        SanctionDTO sanction = sanctionService.getSanctionById(id);

        boolean hasFullAccess = hasAuthority("SANCTION_VIEW");

        if (!hasFullAccess) {
            Employee currentEmployee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Employe non trouve pour l'utilisateur " + userId));

            if (!sanction.getEmployeId().equals(currentEmployee.getId())) {
                log.warn("Acces refuse : l'utilisateur {} tente d'acceder a la sanction {} qui ne le concerne pas",
                        userId, id);
                throw new AccessDeniedException("Vous n'avez pas le droit d'acceder a cette sanction");
            }
        }

        log.info("Sanction {} retournee a l'utilisateur {}", id, userId);
        return ResponseEntity.ok(sanction);
    }

    @GetMapping("/sanctions/employee/{employeeId}")
    @PreAuthorize("hasAnyAuthority('SANCTION_VIEW', 'SANCTION_VIEW_ALL')")
    public ResponseEntity<Page<SanctionDTO>> getSanctionsByEmployee(
            @PathVariable String employeeId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        log.info("Recuperation des sanctions pour l'employe: {}", employeeId);
        Page<SanctionDTO> sanctions = sanctionService.getSanctionsByEmployee(employeeId, pageable);
        log.info("{} sanctions trouvees pour l'employe {}", sanctions.getTotalElements(), employeeId);
        return ResponseEntity.ok(sanctions);
    }

    @GetMapping("/sanctions/me")
    @PreAuthorize("hasAuthority('SANCTION_VIEW_OWN')")
    public ResponseEntity<Page<SanctionDTO>> getMySanctions(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        try {
            String userId = securityUtils.getCurrentUserId();
            log.info("Recuperation des sanctions pour l'utilisateur: {}", userId);

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("Utilisateur non trouve: " + userId));

            Optional<Employee> employeeOpt = employeeRepository.findByUserId(userId);

            if (employeeOpt.isPresent()) {
                Employee employee = employeeOpt.get();
                log.info("Employe trouve par userId: {} - {}", employee.getId(), employee.getNomComplet());

                Page<SanctionDTO> sanctions = sanctionService.getSanctionsByEmployee(employee.getId(), pageable);
                log.info("{} sanctions trouvees pour l'employe {}", sanctions.getTotalElements(), employee.getId());

                return ResponseEntity.ok(sanctions);
            }

            if (user.getEmployeeId() != null && !user.getEmployeeId().isEmpty()) {
                log.info("Recherche par employeeId: {}", user.getEmployeeId());
                Employee employee = employeeRepository.findById(user.getEmployeeId()).orElse(null);

                if (employee != null) {
                    log.info("Employe trouve par employeeId: {} - {}", employee.getId(), employee.getNomComplet());
                    Page<SanctionDTO> sanctions = sanctionService.getSanctionsByEmployee(employee.getId(), pageable);
                    log.info("{} sanctions trouvees", sanctions.getTotalElements());
                    return ResponseEntity.ok(sanctions);
                }
            }

            String userFullName = (user.getFirstName() != null ? user.getFirstName() : "") +
                    " " +
                    (user.getLastName() != null ? user.getLastName() : "");

            if (!userFullName.trim().isEmpty()) {
                log.info("Recherche d'un employe avec le nom: {}", userFullName);

                String[] nameParts = userFullName.trim().split(" ");
                String firstName = nameParts.length > 0 ? nameParts[0] : "";
                String lastName = nameParts.length > 1 ? String.join(" ", java.util.Arrays.copyOfRange(nameParts, 1, nameParts.length)) : "";

                Optional<Employee> employeeByNameOpt = employeeRepository.findAll()
                        .stream()
                        .filter(e -> {
                            boolean matchPrenom = firstName.equalsIgnoreCase(e.getPrenom() != null ? e.getPrenom() : "");
                            boolean matchNom = lastName.equalsIgnoreCase(e.getNom() != null ? e.getNom() : "");
                            return matchPrenom && matchNom;
                        })
                        .findFirst();

                if (employeeByNameOpt.isPresent()) {
                    Employee employee = employeeByNameOpt.get();
                    log.info("Employe trouve par nom: {} - {}", employee.getId(), employee.getNomComplet());

                    employee.setUser(user);
                    employee.setUpdatedBy(userId);
                    employee.setUpdatedAt(LocalDate.now());
                    employee = employeeRepository.save(employee);

                    Page<SanctionDTO> sanctions = sanctionService.getSanctionsByEmployee(employee.getId(), pageable);
                    log.info("{} sanctions trouvees pour l'employe {}", sanctions.getTotalElements(), employee.getId());

                    return ResponseEntity.ok(sanctions);
                }
            }

            log.warn("Aucun employe trouve pour l'utilisateur: {}", userId);
            return ResponseEntity.ok(new PageImpl<>(Collections.emptyList(), pageable, 0));

        } catch (Exception e) {
            log.error("Erreur lors de la recuperation des sanctions: {}", e.getMessage(), e);
            Page<SanctionDTO> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
            return ResponseEntity.ok(emptyPage);
        }
    }

    @PostMapping("/sanctions")
    @PreAuthorize("hasAuthority('SANCTION_CREATE')")
    public ResponseEntity<SanctionDTO> createSanction(@Valid @RequestBody SanctionDTO dto) {
        String userId = securityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(sanctionService.createSanction(dto, userId));
    }

    @PutMapping("/sanctions/{id}")
    @PreAuthorize("hasAuthority('SANCTION_UPDATE')")
    public ResponseEntity<SanctionDTO> updateSanction(
            @PathVariable String id,
            @Valid @RequestBody SanctionDTO dto) {
        String userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(sanctionService.updateSanction(id, dto, userId));
    }

    @PostMapping("/sanctions/{id}/lift")
    @PreAuthorize("hasAuthority('SANCTION_UPDATE')")
    public ResponseEntity<SanctionDTO> liftSanction(@PathVariable String id) {
        String userId = securityUtils.getCurrentUserId();
        return ResponseEntity.ok(sanctionService.leverSanction(id, userId));
    }

    @DeleteMapping("/sanctions/{id}")
    @PreAuthorize("hasAuthority('SANCTION_DELETE')")
    public ResponseEntity<Void> deleteSanction(@PathVariable String id) {
        sanctionService.deleteSanction(id);
        return ResponseEntity.noContent().build();
    }

    // ============================================
    // DIAGNOSTIC
    // ============================================

    @GetMapping("/sanctions/diagnostic")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> diagnosticSanctions() {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            log.info("DIAGNOSTIC - Debut");

            long totalSanctions = sanctionService.getAllSanctions(Pageable.unpaged()).getTotalElements();
            result.put("totalSanctions", totalSanctions);
            log.info("Total sanctions: {}", totalSanctions);

            List<SanctionDTO> allSanctions = sanctionService.getAllSanctions(Pageable.unpaged()).getContent();
            List<Map<String, Object>> sanctionsList = allSanctions.stream()
                    .map(s -> {
                        Map<String, Object> map = new LinkedHashMap<>();
                        map.put("id", s.getId());
                        map.put("numero", s.getNumero());
                        map.put("employeId", s.getEmployeId());
                        map.put("employeNom", s.getEmployeNom());
                        map.put("type", s.getType());
                        map.put("statut", s.getStatut());
                        return map;
                    })
                    .collect(java.util.stream.Collectors.toList());
            result.put("sanctions", sanctionsList);

            String userId = securityUtils.getCurrentUserId();
            result.put("currentUserId", userId);

            User user = userRepository.findById(userId).orElse(null);
            if (user != null) {
                Map<String, Object> userInfo = new LinkedHashMap<>();
                userInfo.put("id", user.getId());
                userInfo.put("firstName", user.getFirstName());
                userInfo.put("lastName", user.getLastName());
                userInfo.put("roleId", user.getRoleId());
                userInfo.put("employeeId", user.getEmployeeId());
                result.put("user", userInfo);
            }

            log.info("DIAGNOSTIC - Fin");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Erreur de diagnostic: {}", e.getMessage(), e);
            result.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    // ============================================
    // UTILITAIRE
    // ============================================

    private boolean hasAuthority(String authority) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(a -> a.equals(authority));
    }
}
package com.fric.sirh.seed;

import com.fric.sirh.model.Role;
import com.fric.sirh.repository.PermissionRepository;
import com.fric.sirh.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class RoleSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    public void run(String... args) {
        if (roleRepository.count() > 0) {
            log.info("Roles already exist");
            return;
        }

        Map<String, String> perms = permissionRepository.findAll()
                .stream()
                .collect(Collectors.toMap(p -> p.getName(), p -> p.getId()));

        LocalDate now = LocalDate.now();

        roleRepository.saveAll(List.of(
                // ======================
                // 1. RH (Ressources Humaines) - Acces complet
                // ======================
                role("RH", 5, "GLOBAL", perms, List.of("*"), now),

                // ======================
                // 2. TOP MANAGER - Peut tout voir mais ne peut rien faire
                // ======================
                role("TOP_MANAGER", 4, "GLOBAL", perms, List.of(
                        "USER_VIEW_ALL", "USER_VIEW",
                        "EMPLOYEE_VIEW_ALL", "EMPLOYEE_VIEW",
                        "COMPANY_VIEW_ALL", "COMPANY_VIEW",
                        "DEPARTMENT_VIEW_ALL", "DEPARTMENT_VIEW",
                        "POSITION_VIEW_ALL", "POSITION_VIEW",
                        "LEAVE_VIEW_ALL", "LEAVE_VIEW_OWN","LEAVE_REJECT",
                        "CONTRACT_VIEW_ALL", "CONTRACT_VIEW",
                        "PAYSLIP_VIEW_ALL", "PAYSLIP_VIEW", "PAYSLIP_DOWNLOAD",
                        "DOC_VIEW_ALL", "DOC_VIEW", "DOC_DOWNLOAD",
                        "SANCTION_VIEW_OWN", "SANCTION_VIEW",
                        "EXPLANATION_REQUEST_VIEW", "EXPLANATION_REQUEST_VIEW_OWN",
                        "HR_REQUEST_VIEW_ALL",
                        "REPORT_VIEW", "REPORT_EXPORT",
                        "PROFILE_VIEW", "PROFILE_VIEW_ANY",
                        "TEAM_VIEW_ALL", "TEAM_VIEW",
                        "PERFORMANCE_VIEW", "PERFORMANCE_VIEW_ALL",
                        "PERFORMANCE_CRITERIA_VIEW",
                        "RANKING_VIEW", "RANKING_VIEW_ALL",
                        "EMPLOYEE_VIEW"
                ), now),

                // ======================
                // 3. DIRECTION - Acces entreprise
                // ======================
                role("DIRECTION", 3, "COMPANY", perms, List.of(
                        "USER_VIEW_ALL", "USER_UPDATE", "USER_VIEW",
                        "EMPLOYEE_VIEW_ALL", "EMPLOYEE_VIEW",
                        "COMPANY_VIEW_ALL", "COMPANY_VIEW",
                        "DEPARTMENT_VIEW_ALL", "DEPARTMENT_VIEW",
                        "POSITION_VIEW_ALL", "POSITION_VIEW",
                        "LEAVE_VIEW_ALL", "LEAVE_APPROVE", "LEAVE_REJECT", "LEAVE_CREATE","LEAVE_VIEW_OWN",
                        "PAYSLIP_VIEW_ALL", "PAYSLIP_VIEW", "PAYSLIP_DOWNLOAD",
                        "DOC_VIEW_ALL", "DOC_VIEW",
                        "CONTRACT_VIEW_ALL", "CONTRACT_VIEW",
                        "SANCTION_VIEW_OWN",
                        "EXPLANATION_REQUEST_VIEW",
                        "EXPLANATION_REQUEST_VIEW_OWN",
                        "EXPLANATION_REQUEST_RESPOND",
                        "EXPLANATION_REQUEST_CREATE",
                        "EXPLANATION_REQUEST_UPDATE",
                        "EXPLANATION_REQUEST_DELETE",
                        "REPORT_VIEW", "REPORT_EXPORT",
                        "PROFILE_VIEW", "PROFILE_UPDATE",
                        "TEAM_VIEW_ALL", "TEAM_VIEW",
                        "PERFORMANCE_VIEW", "PERFORMANCE_UPDATE",
                        "RANKING_VIEW", "RANKING_EXPORT"
                ), now),

                // ======================
                // 4. MANAGER - Acces equipe
                // ======================
                role("MANAGER", 2, "TEAM", perms, List.of(
                        "PROFILE_VIEW", "PROFILE_UPDATE",
                        "POSITION_VIEW",
                        "TEAM_VIEW",
                        "COMPANY_VIEW",
                        "EMPLOYEE_VIEW",
                        "DEPARTMENT_VIEW",
                        "LEAVE_CREATE","LEAVE_VIEW_ALL", "LEAVE_VIEW_OWN", "LEAVE_VIEW_TEAM", "LEAVE_APPROVE", "LEAVE_REJECT",
                        "PAYSLIP_VIEW", "PAYSLIP_VIEW",
                        "DOC_VIEW", "DOC_UPLOAD", "DOC_VIEW",
                        "SANCTION_VIEW_OWN",
                        "USER_UPDATE", "USER_VIEW",
                        "EXPLANATION_REQUEST_VIEW",
                        "EXPLANATION_REQUEST_VIEW_OWN",
                        "EXPLANATION_REQUEST_RESPOND",
                        "EXPLANATION_REQUEST_CREATE",
                        "EXPLANATION_REQUEST_UPDATE",
                        "PERFORMANCE_VIEW",
                        "RANKING_VIEW", "RANKING_VIEW",
                        "CONTRACT_VIEW"
                ), now),

                // ======================
                // 5. EMPLOYEE - Acces personnel uniquement
                // ======================
                role("EMPLOYEE", 1, "SELF", perms, List.of(
                        "PROFILE_VIEW", "PROFILE_UPDATE",
                        "POSITION_VIEW",
                        "LEAVE_CREATE", "LEAVE_VIEW_OWN","LEAVE_REJECT",
                        "PAYSLIP_VIEW",
                        "DEPARTMENT_VIEW",
                        "DOC_VIEW", "DOC_UPLOAD",
                        "SANCTION_VIEW_OWN",
                        "COMPANY_VIEW",
                        "USER_UPDATE", "USER_VIEW",
                        "EXPLANATION_REQUEST_VIEW_OWN",
                        "EXPLANATION_REQUEST_RESPOND",
                        "PERFORMANCE_VIEW", "EMPLOYEE_VIEW",
                        "RANKING_VIEW",
                        // Ajout pour permettre la consultation de son propre contrat
                        "CONTRACT_VIEW"
                ), now)
        ));

        log.info("Roles seeded cleanly with all permissions");
        log.info("Roles created: RH, TOP_MANAGER, DIRECTION, MANAGER, EMPLOYEE");
        log.info("RH: Full access to all features");
        log.info("TOP_MANAGER: Can view everything but cannot perform any action");
        log.info("DIRECTION: Can view, create and modify performances");
        log.info("MANAGER: Can view and create performances for their team");
        log.info("EMPLOYEE: Can only view their own performances and contracts");
    }

    private Role role(String name, int level, String scope,
                      Map<String, String> perms,
                      List<String> names,
                      LocalDate now) {
        List<String> ids = names.stream()
                .filter(n -> !n.equals("*"))
                .map(perms::get)
                .filter(v -> v != null)
                .collect(Collectors.toList());

        if (names.contains("*")) {
            ids = perms.values().stream().toList();
        }

        return Role.builder()
                .name(name)
                .hierarchyLevel(level)
                .visibilityScope(scope)
                .permissionIds(ids)
                .active(true)
                .createdAt(now)
                .createdBy("SYSTEM")
                .build();
    }
}
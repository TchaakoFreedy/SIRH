package com.fric.sirh.config;

import com.fric.sirh.model.Permission;
import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.PermissionRepository;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        log.info("🚀 Starting data initialization...");
        initializePermissions();
        initializeRoles();
        // ❌ SUPPRIMÉ : initializeAdminUser();
        log.info("✅ Data initialization completed!");
    }

    private void initializePermissions() {
        List<String> permissionNames = Arrays.asList(
                // ============================================
                // 1. UTILISATEURS (USER)
                // ============================================
                "USER_VIEW_ALL", "USER_VIEW", "USER_CREATE", "USER_UPDATE", "USER_DELETE",

                // ============================================
                // 2. EMPLOYÉS (EMPLOYEE)
                // ============================================
                "EMPLOYEE_VIEW_ALL", "EMPLOYEE_VIEW", "EMPLOYEE_CREATE", "EMPLOYEE_UPDATE",
                "EMPLOYEE_DELETE", "EMPLOYEE_SUSPEND", "EMPLOYEE_REACTIVATE",

                // ============================================
                // 3. ENTREPRISES (COMPANY)
                // ============================================
                "COMPANY_VIEW", "COMPANY_VIEW_ALL", "COMPANY_CREATE", "COMPANY_UPDATE",
                "COMPANY_DELETE", "COMPANY_SUSPEND", "COMPANY_REACTIVATE",

                // ============================================
                // 4. DÉPARTEMENTS (DEPARTMENT)
                // ============================================
                "DEPARTMENT_VIEW", "DEPARTMENT_VIEW_ALL", "DEPARTMENT_CREATE", "DEPARTMENT_UPDATE",
                "DEPARTMENT_DELETE", "DEPARTMENT_SUSPEND", "DEPARTMENT_REACTIVATE",

                // ============================================
                // 5. POSTES (POSITION)
                // ============================================
                "POSITION_VIEW", "POSITION_VIEW_ALL", "POSITION_CREATE", "POSITION_UPDATE",
                "POSITION_DELETE", "POSITION_TOGGLE",

                // ============================================
                // 6. PROFIL (PROFILE)
                // ============================================
                "PROFILE_VIEW", "PROFILE_UPDATE", "PROFILE_VIEW_ANY", "PROFILE_UPDATE_ANY",

                // ============================================
                // 7. CONGÉS (LEAVE)
                // ============================================
                "LEAVE_CREATE", "LEAVE_VIEW_OWN", "LEAVE_VIEW_TEAM", "LEAVE_VIEW_ALL",
                "LEAVE_APPROVE", "LEAVE_REJECT", "LEAVE_CANCEL", "LEAVE_DELETE",

                // ============================================
                // 8. FICHES DE PAIE (PAYROLL)
                // ============================================
                "PAYSLIP_VIEW", "PAYSLIP_VIEW_ALL", "PAYSLIP_CREATE", "PAYSLIP_UPDATE", "PAYSLIP_DELETE",

                // ============================================
                // 9. DOCUMENTS (DOC)
                // ============================================
                "DOC_VIEW", "DOC_VIEW_ALL", "DOC_UPLOAD", "DOC_DELETE", "DOC_DOWNLOAD", "DOC_SHARE",

                // ============================================
                // 10. CONTRATS (CONTRACT)
                // ============================================
                "CONTRACT_VIEW", "CONTRACT_VIEW_ALL", "CONTRACT_CREATE", "CONTRACT_UPDATE",
                "CONTRACT_DELETE", "CONTRACT_SIGN", "CONTRACT_TERMINATE",

                // ============================================
                // 11. SANCTIONS (SANCTION)
                // ============================================
                "SANCTION_VIEW", "SANCTION_VIEW_ALL", "SANCTION_CREATE", "SANCTION_UPDATE", "SANCTION_DELETE",

                // ============================================
                // 12. DEMANDES RH (HR_REQUEST)
                // ============================================
                "HR_REQUEST_CREATE", "HR_REQUEST_VIEW_OWN", "HR_REQUEST_VIEW_ALL",
                "HR_REQUEST_APPROVE", "HR_REQUEST_REJECT",

                // ============================================
                // 13. DEMANDES D'EXPLICATION (EXPLANATION)
                // ============================================
                "EXPLANATION_REQUEST_VIEW", "EXPLANATION_REQUEST_RESPOND",
                "EXPLANATION_REQUEST_CREATE", "EXPLANATION_REQUEST_DELETE",

                // ============================================
                // 14. ÉQUIPES (TEAM)
                // ============================================
                "TEAM_VIEW", "TEAM_VIEW_ALL", "TEAM_CREATE", "TEAM_UPDATE", "TEAM_DELETE",

                // ============================================
                // 15. RAPPORTS (REPORT)
                // ============================================
                "REPORT_VIEW", "REPORT_EXPORT", "REPORT_CREATE",

                // ============================================
                // 16. ADMINISTRATION (ADMIN)
                // ============================================
                "ROLE_VIEW", "ROLE_CREATE", "ROLE_UPDATE", "ROLE_DELETE",
                "PERMISSION_VIEW", "PERMISSION_UPDATE",
                "USER_PERMISSION_VIEW", "USER_PERMISSION_EDIT",

                // ============================================
                // 17. SYSTÈME (SYSTEM) - Supprimé SUPER_ADMIN
                // ============================================
                "SYSTEM_CONFIG", "SYSTEM_LOGS", "SYSTEM_BACKUP",

                // ============================================
                // 18. NOTIFICATIONS
                // ============================================
                "NOTIFICATION_VIEW", "NOTIFICATION_SEND", "NOTIFICATION_VIEW_ALL",

                // ============================================
                // 19. CALENDRIER
                // ============================================
                "CALENDAR_VIEW", "CALENDAR_EDIT", "CALENDAR_VIEW_ALL"
        );

        for (String name : permissionNames) {
            if (!permissionRepository.existsByName(name)) {
                Permission permission = new Permission();
                permission.setName(name);
                permission.setDescription("Permission: " + name);
                permission.setCategory(determineCategory(name));
                permission.setRequiredLevel(determineRequiredLevel(name));
                permissionRepository.save(permission);
                log.debug("Created permission: {}", name);
            }
        }
        log.info("✅ Permissions initialized successfully");
    }

    /**
     * Détermine la catégorie d'une permission
     */
    private String determineCategory(String name) {
        if (name.startsWith("USER_")) return "USER";
        if (name.startsWith("EMPLOYEE_")) return "EMPLOYEE";
        if (name.startsWith("COMPANY_")) return "COMPANY";
        if (name.startsWith("DEPARTMENT_")) return "DEPARTMENT";
        if (name.startsWith("POSITION_")) return "POSITION";
        if (name.startsWith("PROFILE_")) return "PROFILE";
        if (name.startsWith("LEAVE_")) return "LEAVE";
        if (name.startsWith("PAYSLIP_")) return "PAYROLL";
        if (name.startsWith("DOC_")) return "DOCUMENT";
        if (name.startsWith("CONTRACT_")) return "CONTRACT";
        if (name.startsWith("SANCTION_")) return "SANCTION";
        if (name.startsWith("HR_REQUEST_")) return "HR_REQUEST";
        if (name.startsWith("EXPLANATION_")) return "EXPLANATION";
        if (name.startsWith("TEAM_")) return "TEAM";
        if (name.startsWith("REPORT_")) return "REPORT";
        if (name.startsWith("ROLE_") || name.startsWith("PERMISSION_") || name.startsWith("USER_PERMISSION_")) return "ADMIN";
        if (name.startsWith("SYSTEM_")) return "SYSTEM";
        if (name.startsWith("NOTIFICATION_")) return "NOTIFICATION";
        if (name.startsWith("CALENDAR_")) return "CALENDAR";
        return "OTHER";
    }

    /**
     * Détermine le niveau requis pour une permission
     */
    private int determineRequiredLevel(String name) {
        if (name.endsWith("_ALL") || name.endsWith("_VIEW_ALL")) return 2;
        if (name.endsWith("_CREATE") || name.endsWith("_UPDATE") || name.endsWith("_EDIT")) return 2;
        if (name.endsWith("_DELETE") || name.endsWith("_MANAGE")) return 3;
        if (name.startsWith("SYSTEM_")) return 4;
        if (name.startsWith("ROLE_") || name.startsWith("PERMISSION_")) return 3;
        return 1;
    }

    private void initializeRoles() {
        // ❌ SUPPRIMÉ : SYSTEM_ADMIN role

        // Create RH role
        createRoleIfNotExists(
                "RH",
                "Human Resources Manager - Full Access",
                getAllPermissions()
        );

        // Create DIRECTION role
        createRoleIfNotExists(
                "DIRECTION",
                "Direction",
                getDirectionPermissions()
        );

        // Create TOP_MANAGER role
        createRoleIfNotExists(
                "TOP_MANAGER",
                "Top Manager",
                getTopManagerPermissions()
        );

        // Create MANAGER role
        createRoleIfNotExists(
                "MANAGER",
                "Manager",
                getManagerPermissions()
        );

        // Create EMPLOYEE role
        createRoleIfNotExists(
                "EMPLOYEE",
                "Employee",
                getEmployeePermissions()
        );

        log.info("✅ Roles initialized successfully");
    }

    private void createRoleIfNotExists(String name, String description, Set<Permission> permissions) {
        try {
            java.util.Optional<Role> roleOptional = roleRepository.findByName(name);
            Role role = roleOptional.orElse(null);

            List<String> permissionIds = permissions.stream()
                    .map(Permission::getId)
                    .collect(Collectors.toList());

            if (role == null) {
                role = new Role();
                role.setName(name);
                role.setDescription(description);
                role.setPermissionIds(permissionIds);
                roleRepository.save(role);
                log.info("Created role: {} with {} permissions", name, permissionIds.size());
            } else {
                if (role.getPermissionIds() == null || role.getPermissionIds().isEmpty()) {
                    role.setPermissionIds(permissionIds);
                    roleRepository.save(role);
                    log.info("Updated permissions for role: {} with {} permissions", name, permissionIds.size());
                } else {
                    log.debug("Role {} already exists with permissions", name);
                }
            }
        } catch (Exception e) {
            log.error("Error creating role {}: {}", name, e.getMessage());
        }
    }

    private Set<Permission> getAllPermissions() {
        Set<Permission> allPermissions = new HashSet<>(permissionRepository.findAll());
        log.debug("Retrieved {} permissions for RH", allPermissions.size());
        return allPermissions;
    }

    private Set<Permission> getRHPermissions() {
        List<String> permissionNames = Arrays.asList(
                // ✅ Toutes les permissions pour le RH
                "USER_VIEW_ALL", "USER_CREATE", "USER_UPDATE", "USER_DELETE",
                "EMPLOYEE_VIEW_ALL", "EMPLOYEE_CREATE", "EMPLOYEE_UPDATE", "EMPLOYEE_DELETE", "EMPLOYEE_SUSPEND", "EMPLOYEE_REACTIVATE",
                "COMPANY_VIEW", "COMPANY_VIEW_ALL", "COMPANY_CREATE", "COMPANY_UPDATE", "COMPANY_DELETE", "COMPANY_SUSPEND", "COMPANY_REACTIVATE",
                "DEPARTMENT_VIEW", "DEPARTMENT_VIEW_ALL", "DEPARTMENT_CREATE", "DEPARTMENT_UPDATE", "DEPARTMENT_DELETE", "DEPARTMENT_SUSPEND", "DEPARTMENT_REACTIVATE",
                "POSITION_VIEW", "POSITION_VIEW_ALL", "POSITION_CREATE", "POSITION_UPDATE", "POSITION_DELETE", "POSITION_TOGGLE",
                "PROFILE_VIEW", "PROFILE_UPDATE",
                "LEAVE_CREATE", "LEAVE_VIEW_OWN", "LEAVE_VIEW_TEAM", "LEAVE_VIEW_ALL", "LEAVE_APPROVE", "LEAVE_REJECT",
                "PAYSLIP_VIEW", "PAYSLIP_VIEW_ALL", "PAYSLIP_CREATE",
                "DOC_VIEW", "DOC_VIEW_ALL", "DOC_UPLOAD",
                "CONTRACT_VIEW", "CONTRACT_VIEW_ALL", "CONTRACT_CREATE", "CONTRACT_UPDATE", "CONTRACT_DELETE",
                "SANCTION_VIEW", "SANCTION_VIEW_ALL", "SANCTION_CREATE",
                "HR_REQUEST_CREATE", "HR_REQUEST_VIEW_OWN", "HR_REQUEST_VIEW_ALL",
                "EXPLANATION_REQUEST_VIEW", "EXPLANATION_REQUEST_RESPOND", "EXPLANATION_REQUEST_CREATE",
                "TEAM_VIEW", "TEAM_VIEW_ALL",
                "REPORT_VIEW", "REPORT_EXPORT",
                // ✅ Permissions ADMIN pour le RH
                "ROLE_VIEW", "PERMISSION_VIEW", "USER_PERMISSION_VIEW"
        );
        return getPermissionsByNames(permissionNames);
    }

    private Set<Permission> getDirectionPermissions() {
        List<String> permissionNames = Arrays.asList(
                "USER_VIEW_ALL",
                "EMPLOYEE_VIEW_ALL", "EMPLOYEE_VIEW",
                "COMPANY_VIEW_ALL", "COMPANY_VIEW",
                "DEPARTMENT_VIEW_ALL", "DEPARTMENT_VIEW",
                "POSITION_VIEW_ALL", "POSITION_VIEW",
                "LEAVE_VIEW_ALL", "LEAVE_VIEW_TEAM", "LEAVE_APPROVE", "LEAVE_REJECT", "LEAVE_CREATE",
                "PAYSLIP_VIEW_ALL", "PAYSLIP_VIEW",
                "DOC_VIEW_ALL", "DOC_VIEW", "DOC_UPLOAD",
                "CONTRACT_VIEW",
                "SANCTION_VIEW_ALL", "SANCTION_VIEW",
                "REPORT_VIEW", "REPORT_EXPORT",
                "PROFILE_VIEW", "PROFILE_UPDATE",
                "TEAM_VIEW_ALL"
        );
        return getPermissionsByNames(permissionNames);
    }

    private Set<Permission> getTopManagerPermissions() {
        List<String> permissionNames = Arrays.asList(
                "USER_VIEW_ALL", "USER_UPDATE", "USER_VIEW",
                "EMPLOYEE_VIEW_ALL", "EMPLOYEE_VIEW",
                "COMPANY_VIEW_ALL", "COMPANY_VIEW",
                "DEPARTMENT_VIEW_ALL", "DEPARTMENT_VIEW",
                "POSITION_VIEW_ALL", "POSITION_VIEW",
                "LEAVE_VIEW_ALL", "LEAVE_CREATE",
                "CONTRACT_VIEW_ALL",
                "PAYSLIP_VIEW_ALL",
                "DOC_VIEW_ALL", "DOC_VIEW",
                "SANCTION_VIEW_ALL",
                "HR_REQUEST_VIEW_ALL",
                "REPORT_VIEW", "REPORT_EXPORT",
                "PROFILE_VIEW", "PROFILE_UPDATE",
                // ✅ Permissions ADMIN pour TOP_MANAGER
                "ROLE_VIEW", "PERMISSION_VIEW", "USER_PERMISSION_VIEW"
        );
        return getPermissionsByNames(permissionNames);
    }

    private Set<Permission> getManagerPermissions() {
        List<String> permissionNames = Arrays.asList(
                "PROFILE_VIEW", "PROFILE_UPDATE",
                "POSITION_VIEW",
                "TEAM_VIEW",
                "COMPANY_VIEW",
                "EMPLOYEE_VIEW",
                "DEPARTMENT_VIEW",
                "LEAVE_CREATE", "LEAVE_VIEW_OWN", "LEAVE_VIEW_TEAM", "LEAVE_APPROVE", "LEAVE_REJECT", "LEAVE_VIEW_ALL",
                "PAYSLIP_VIEW",
                "DOC_VIEW", "DOC_UPLOAD", "DOC_VIEW_ALL",
                "SANCTION_CREATE", "SANCTION_VIEW",
                "USER_UPDATE", "USER_VIEW",
                "EXPLANATION_REQUEST_CREATE", "EXPLANATION_REQUEST_VIEW", "EXPLANATION_REQUEST_RESPOND"
        );
        return getPermissionsByNames(permissionNames);
    }

    private Set<Permission> getEmployeePermissions() {
        List<String> permissionNames = Arrays.asList(
                "PROFILE_VIEW", "PROFILE_UPDATE",
                "POSITION_VIEW",
                "LEAVE_CREATE", "LEAVE_VIEW_OWN",
                "PAYSLIP_VIEW",
                "DEPARTMENT_VIEW",
                "DOC_VIEW", "DOC_UPLOAD",
                "SANCTION_VIEW",
                "COMPANY_VIEW",
                "USER_UPDATE", "USER_VIEW",
                "EXPLANATION_REQUEST_VIEW", "EXPLANATION_REQUEST_RESPOND"
        );
        return getPermissionsByNames(permissionNames);
    }

    private Set<Permission> getPermissionsByNames(List<String> names) {
        Set<Permission> permissions = new HashSet<>();
        for (String name : names) {
            try {
                java.util.Optional<Permission> permissionOptional = permissionRepository.findByName(name);
                permissionOptional.ifPresent(permissions::add);
            } catch (Exception e) {
                log.warn("Permission not found: {}", name);
            }
        }
        return permissions;
    }

    //  SUPPRIMÉ : initializeAdminUser() méthode entièrement supprimée
}
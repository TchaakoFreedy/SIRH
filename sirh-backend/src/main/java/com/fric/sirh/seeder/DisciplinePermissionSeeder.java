package com.fric.sirh.seeder;

import com.fric.sirh.model.Permission;
import com.fric.sirh.model.Role;
import com.fric.sirh.repository.PermissionRepository;
import com.fric.sirh.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(3)
public class DisciplinePermissionSeeder implements CommandLineRunner {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        try {
            log.info("🚀 Début du seeding des permissions Discipline & Performance...");

            // Créer les permissions du module Discipline
            List<String> disciplinePermissions = Arrays.asList(
                    "DISCIPLINE_VIEW",
                    "DISCIPLINE_CREATE",
                    "DISCIPLINE_UPDATE",
                    "DISCIPLINE_DELETE",
                    "DISCIPLINE_VALIDATE",
                    "DISCIPLINE_RESPOND",
                    "SANCTION_VIEW",
                    "SANCTION_CREATE",
                    "SANCTION_UPDATE",
                    "SANCTION_DELETE"
            );

            // Créer les permissions du module Performance
            List<String> performancePermissions = Arrays.asList(
                    "PERFORMANCE_VIEW",
                    "PERFORMANCE_CREATE",
                    "PERFORMANCE_UPDATE",
                    "PERFORMANCE_DELETE",
                    "PERFORMANCE_EXPORT"
            );

            List<String> allPermissions = new ArrayList<>();
            allPermissions.addAll(disciplinePermissions);
            allPermissions.addAll(performancePermissions);

            // Créer les permissions si elles n'existent pas
            for (String permissionName : allPermissions) {
                if (!permissionRepository.existsByName(permissionName)) {
                    Permission permission = Permission.builder()
                            .name(permissionName)
                            .description("Permission: " + permissionName)
                            .category(getCategoryForPermission(permissionName))
                            .requiredLevel(getRequiredLevelForPermission(permissionName))
                            .active(true)
                            .createdBy("SYSTEM")
                            .createdAt(LocalDate.now())
                            .build();
                    permissionRepository.save(permission);
                    log.info("✅ Permission créée: {}", permissionName);
                }
            }

            // Assigner les permissions aux rôles
            assignPermissionsToRoles();

            log.info("✅ Seeding des permissions Discipline & Performance terminé avec succès");

        } catch (Exception e) {
            log.error("❌ Erreur lors du seeding des permissions: {}", e.getMessage(), e);
        }
    }

    private void assignPermissionsToRoles() {
        try {
            // Récupérer toutes les permissions
            List<Permission> allPermissionsList = permissionRepository.findAll();
            List<String> allPermissionIds = allPermissionsList.stream()
                    .map(Permission::getId)
                    .collect(Collectors.toList());

            // RH - Toutes les permissions
            Role rhRole = roleRepository.findByName("RH").orElse(null);
            if (rhRole != null) {
                rhRole.setPermissionIds(allPermissionIds);
                roleRepository.save(rhRole);
                log.info("✅ Toutes les permissions assignées au rôle RH ({} permissions)", allPermissionIds.size());
            }

            // Direction - Permissions de visualisation et création
            Role directionRole = roleRepository.findByName("Direction").orElse(null);
            if (directionRole != null) {
                List<String> directionPermissionNames = Arrays.asList(
                        "DISCIPLINE_VIEW",
                        "DISCIPLINE_CREATE",
                        "DISCIPLINE_UPDATE",
                        "DISCIPLINE_VALIDATE",
                        "SANCTION_VIEW",
                        "SANCTION_CREATE",
                        "PERFORMANCE_VIEW",
                        "PERFORMANCE_CREATE"
                );
                List<String> directionPermissionIds = allPermissionsList.stream()
                        .filter(p -> directionPermissionNames.contains(p.getName()))
                        .map(Permission::getId)
                        .collect(Collectors.toList());
                directionRole.setPermissionIds(directionPermissionIds);
                roleRepository.save(directionRole);
                log.info("✅ Permissions assignées au rôle Direction ({} permissions)", directionPermissionIds.size());
            }

            // Manager - Permissions de visualisation et création limitées
            Role managerRole = roleRepository.findByName("Manager").orElse(null);
            if (managerRole != null) {
                List<String> managerPermissionNames = Arrays.asList(
                        "DISCIPLINE_VIEW",
                        "DISCIPLINE_CREATE",
                        "DISCIPLINE_UPDATE",
                        "DISCIPLINE_RESPOND",
                        "SANCTION_VIEW",
                        "PERFORMANCE_VIEW"
                );
                List<String> managerPermissionIds = allPermissionsList.stream()
                        .filter(p -> managerPermissionNames.contains(p.getName()))
                        .map(Permission::getId)
                        .collect(Collectors.toList());
                managerRole.setPermissionIds(managerPermissionIds);
                roleRepository.save(managerRole);
                log.info("✅ Permissions assignées au rôle Manager ({} permissions)", managerPermissionIds.size());
            }

            // Employé - Permissions de visualisation et réponse
            Role employeeRole = roleRepository.findByName("Employé").orElse(null);
            if (employeeRole != null) {
                List<String> employeePermissionNames = Arrays.asList(
                        "DISCIPLINE_VIEW",
                        "DISCIPLINE_RESPOND",
                        "PERFORMANCE_VIEW"
                );
                List<String> employeePermissionIds = allPermissionsList.stream()
                        .filter(p -> employeePermissionNames.contains(p.getName()))
                        .map(Permission::getId)
                        .collect(Collectors.toList());
                employeeRole.setPermissionIds(employeePermissionIds);
                roleRepository.save(employeeRole);
                log.info("✅ Permissions assignées au rôle Employé ({} permissions)", employeePermissionIds.size());
            }

        } catch (Exception e) {
            log.error("❌ Erreur lors de l'assignation des permissions: {}", e.getMessage(), e);
        }
    }

    private String getCategoryForPermission(String permissionName) {
        if (permissionName.startsWith("DISCIPLINE") || permissionName.startsWith("SANCTION")) {
            return "DISCIPLINE";
        } else if (permissionName.startsWith("PERFORMANCE")) {
            return "PERFORMANCE";
        }
        return "GENERAL";
    }

    private Integer getRequiredLevelForPermission(String permissionName) {
        if (permissionName.endsWith("_DELETE") || permissionName.endsWith("_EXPORT")) {
            return 1;
        } else if (permissionName.endsWith("_CREATE") || permissionName.endsWith("_UPDATE") || permissionName.endsWith("_VALIDATE")) {
            return 2;
        } else if (permissionName.endsWith("_VIEW") || permissionName.endsWith("_RESPOND")) {
            return 3;
        }
        return 2;
    }
}
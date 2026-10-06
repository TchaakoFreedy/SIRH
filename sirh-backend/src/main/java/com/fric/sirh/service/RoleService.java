package com.fric.sirh.service;

import com.fric.sirh.dto.CreateRoleRequest;
import com.fric.sirh.dto.RoleDto;
import com.fric.sirh.dto.RolePermissionsUpdateRequest;
import com.fric.sirh.exception.BusinessException;
import com.fric.sirh.exception.ResourceNotFoundException;
import com.fric.sirh.mapper.RoleMapper;
import com.fric.sirh.model.Role;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionService permissionService;
    private final RoleMapper roleMapper;
    private final NotificationPublisher notificationPublisher;

    public List<RoleDto> getAllRoles() {
        return roleRepository.findAll()
                .stream()
                .map(roleMapper::toDto)
                .collect(Collectors.toList());
    }

    public RoleDto getRoleById(String id) {
        Role role = findRoleById(id);
        return roleMapper.toDto(role);
    }

    @Transactional
    public RoleDto createRole(CreateRoleRequest request) {
        if (roleRepository.existsByName(request.getName())) {
            throw new BusinessException("Un rôle avec le nom '" + request.getName() + "' existe déjà");
        }

        List<String> permissionIds = request.getPermissionIds() != null
                ? request.getPermissionIds()
                : new ArrayList<>();

        validatePermissions(permissionIds);

        Role role = Role.builder()
                .name(request.getName())
                .description(request.getDescription())
                .hierarchyLevel(request.getHierarchyLevel())
                .permissionIds(permissionIds)
                .visibilityScope(request.getVisibilityScope())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdAt(LocalDate.now())
                .createdBy("SYSTEM")
                .build();

        Role saved = roleRepository.save(role);
        log.info("✅ Rôle créé : {}", saved.getName());

        // 🔔 Notification
        publishRoleEvent(saved, "CREATE", "SYSTEM");

        return roleMapper.toDto(saved);
    }

    @Transactional
    public RoleDto updateRole(String id, CreateRoleRequest request) {
        Role role = findRoleById(id);

        if (request.getName() != null && !request.getName().equals(role.getName())) {
            if (roleRepository.existsByName(request.getName())) {
                throw new BusinessException("Un rôle avec le nom '" + request.getName() + "' existe déjà");
            }
            role.setName(request.getName());
        }

        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }
        if (request.getHierarchyLevel() != null) {
            role.setHierarchyLevel(request.getHierarchyLevel());
        }
        if (request.getVisibilityScope() != null) {
            role.setVisibilityScope(request.getVisibilityScope());
        }
        if (request.getActive() != null) {
            role.setActive(request.getActive());
        }
        if (request.getPermissionIds() != null) {
            validatePermissions(request.getPermissionIds());
            role.setPermissionIds(request.getPermissionIds());
        }

        role.setUpdatedAt(LocalDate.now());
        role.setUpdatedBy("SYSTEM");

        Role updated = roleRepository.save(role);
        log.info("✅ Rôle mis à jour : {}", updated.getName());

        // 🔔 Notification
        publishRoleEvent(updated, "UPDATE", "SYSTEM");

        return roleMapper.toDto(updated);
    }

    @Transactional
    public void deleteRole(String id) {
        Role role = findRoleById(id);
        roleRepository.delete(role);
        log.info("🗑️ Rôle supprimé : {}", role.getName());

        // 🔔 Notification
        publishRoleEvent(role, "DELETE", "SYSTEM");
    }

    @Transactional
    public RoleDto updateRolePermissions(String id, RolePermissionsUpdateRequest request) {
        Role role = findRoleById(id);

        validatePermissions(request.getPermissionIds());

        role.setPermissionIds(request.getPermissionIds());
        role.setUpdatedAt(LocalDate.now());
        role.setUpdatedBy("SYSTEM");

        Role updated = roleRepository.save(role);
        log.info("✅ Permissions du rôle {} mises à jour : {} permissions",
                role.getName(), request.getPermissionIds().size());

        // 🔔 Notification (on considère comme une mise à jour du rôle)
        publishRoleEvent(updated, "PERMISSION_UPDATE", "SYSTEM");

        return roleMapper.toDto(updated);
    }

    public Role findRoleById(String id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rôle", id));
    }

    private void validatePermissions(List<String> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }

        List<String> invalidIds = permissionIds.stream()
                .filter(id -> !permissionService.existsById(id))
                .collect(Collectors.toList());

        if (!invalidIds.isEmpty()) {
            throw new BusinessException("Permissions invalides : " + invalidIds);
        }
    }

    public boolean existsById(String id) {
        return roleRepository.existsById(id);
    }

    // ==========================================
    // MÉTHODE DE PUBLICATION DES NOTIFICATIONS
    // ==========================================

    private void publishRoleEvent(Role role, String action, String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityId", role.getId());
        data.put("entityType", "ROLE");
        data.put("actionUrl", "/roles/" + role.getId());
        data.put("metadata", Map.of(
                "name", role.getName(),
                "hierarchyLevel", role.getHierarchyLevel(),
                "action", action
        ));

        NotificationEvent event = switch (action) {
            case "CREATE" -> NotificationEvent.ROLE_CREATED;
            case "UPDATE", "PERMISSION_UPDATE" -> NotificationEvent.ROLE_UPDATED;
            case "DELETE" -> NotificationEvent.ROLE_DELETED;
            default -> NotificationEvent.SYSTEM;
        };

        notificationPublisher.publish(event, data, userId != null ? userId : "SYSTEM");
    }
}
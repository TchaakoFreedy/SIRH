package com.fric.sirh.service;

import com.fric.sirh.dto.CreatePermissionRequest;
import com.fric.sirh.dto.PermissionDto;
import com.fric.sirh.dto.UpdatePermissionRequest;
import com.fric.sirh.exception.BusinessException;
import com.fric.sirh.exception.ResourceNotFoundException;
import com.fric.sirh.mapper.PermissionMapper;
import com.fric.sirh.model.Permission;
import com.fric.sirh.notification.enums.NotificationEvent;
import com.fric.sirh.notification.publisher.NotificationPublisher;
import com.fric.sirh.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final NotificationPublisher notificationPublisher;

    public List<PermissionDto> getAllPermissions() {
        return permissionRepository.findAll()
                .stream()
                .map(permissionMapper::toDto)
                .collect(Collectors.toList());
    }

    public PermissionDto getPermissionById(String id) {
        Permission permission = findPermissionById(id);
        return permissionMapper.toDto(permission);
    }

    @Transactional
    public PermissionDto createPermission(CreatePermissionRequest request) {
        if (permissionRepository.existsByName(request.getName())) {
            throw new BusinessException("Une permission avec le nom '" + request.getName() + "' existe déjà");
        }

        Permission permission = Permission.builder()
                .name(request.getName())
                .description(request.getDescription())
                .category(request.getCategory())
                .requiredLevel(request.getRequiredLevel())
                .active(request.getActive() != null ? request.getActive() : true)
                .createdAt(LocalDate.now())
                .createdBy("SYSTEM")
                .build();

        Permission saved = permissionRepository.save(permission);
        log.info("✅ Permission créée : {}", saved.getName());

        // 🔔 Notification
        publishPermissionEvent(saved, "CREATE", "SYSTEM");

        return permissionMapper.toDto(saved);
    }

    @Transactional
    public PermissionDto updatePermission(String id, UpdatePermissionRequest request) {
        Permission permission = findPermissionById(id);

        if (request.getDescription() != null) {
            permission.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            permission.setCategory(request.getCategory());
        }
        if (request.getRequiredLevel() != null) {
            permission.setRequiredLevel(request.getRequiredLevel());
        }
        if (request.getActive() != null) {
            permission.setActive(request.getActive());
        }

        permission.setUpdatedAt(LocalDate.now());
        permission.setUpdatedBy("SYSTEM");

        Permission updated = permissionRepository.save(permission);
        log.info("✅ Permission mise à jour : {}", updated.getName());

        // 🔔 Notification
        publishPermissionEvent(updated, "UPDATE", "SYSTEM");

        return permissionMapper.toDto(updated);
    }

    @Transactional
    public void deletePermission(String id) {
        Permission permission = findPermissionById(id);
        permissionRepository.delete(permission);
        log.info("🗑️ Permission supprimée : {}", permission.getName());

        // 🔔 Notification
        publishPermissionEvent(permission, "DELETE", "SYSTEM");
    }

    public Permission findPermissionById(String id) {
        return permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", id));
    }

    public List<Permission> findPermissionsByIds(List<String> ids) {
        return permissionRepository.findByIdIn(ids);
    }

    public boolean existsById(String id) {
        return permissionRepository.existsById(id);
    }

    // ==========================================
    // MÉTHODE DE PUBLICATION DES NOTIFICATIONS
    // ==========================================

    private void publishPermissionEvent(Permission permission, String action, String userId) {
        Map<String, Object> data = new HashMap<>();
        data.put("entityId", permission.getId());
        data.put("entityType", "PERMISSION");
        data.put("actionUrl", "/permissions/" + permission.getId());
        data.put("metadata", Map.of(
                "name", permission.getName(),
                "category", permission.getCategory(),
                "action", action
        ));
        notificationPublisher.publish(NotificationEvent.PERMISSION_UPDATED, data, userId);
    }
}
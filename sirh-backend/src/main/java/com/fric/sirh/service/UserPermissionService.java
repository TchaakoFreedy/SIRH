package com.fric.sirh.service;

import com.fric.sirh.dto.PermissionDto;
import com.fric.sirh.dto.RoleDto;
import com.fric.sirh.dto.UserPermissionsResponse;
import com.fric.sirh.dto.UserPermissionsUpdateRequest;
import com.fric.sirh.exception.BusinessException;
import com.fric.sirh.exception.ResourceNotFoundException;
import com.fric.sirh.mapper.PermissionMapper;
import com.fric.sirh.mapper.RoleMapper;
import com.fric.sirh.model.Permission;
import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.PermissionRepository;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPermissionService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PermissionMapper permissionMapper;
    private final RoleMapper roleMapper;

    /**
     * Calcule les IDs des permissions effectives d'un utilisateur
     */
    public Set<String> calculateEffectivePermissions(User user) {
        if (user == null) {
            return Collections.emptySet();
        }

        Set<String> permissions = new HashSet<>();

        // 1. Récupérer les permissions du rôle (si l'utilisateur a un rôle)
        if (user.getRoleId() != null) {
            Optional<Role> roleOpt = roleRepository.findById(user.getRoleId());
            if (roleOpt.isPresent() && roleOpt.get().getPermissionIds() != null) {
                permissions.addAll(roleOpt.get().getPermissionIds());
            } else {
                log.warn("Rôle non trouvé ou sans permissions pour l'utilisateur: {}", user.getId());
            }
        }

        // 2. Ajouter les permissions accordées individuellement
        if (user.getGrantedPermissionIds() != null) {
            permissions.addAll(user.getGrantedPermissionIds());
        }

        // 3. Retirer les permissions révoquées
        if (user.getRevokedPermissionIds() != null) {
            permissions.removeAll(user.getRevokedPermissionIds());
        }

        return permissions;
    }

    /**
     * Calcule les NOMS (Codes) des permissions effectives pour Spring Security
     */
    public Set<String> calculateEffectivePermissionNames(User user) {
        Set<String> effectiveIds = calculateEffectivePermissions(user);
        if (effectiveIds.isEmpty()) {
            return Collections.emptySet();
        }

        // Convertir les IDs de permission en NOMS (ex: "EMPLOYEE_READ") en 1 seule requête
        return permissionRepository.findByIdIn(new ArrayList<>(effectiveIds))
                .stream()
                .map(Permission::getName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * Calcule les permissions effectives pour un utilisateur donné
     */
    public Set<String> calculateEffectivePermissionsByUserId(String userId) {
        User user = findUserById(userId);
        return calculateEffectivePermissions(user);
    }

    /**
     * Récupère les détails complets des permissions d'un utilisateur
     */
    public UserPermissionsResponse getUserPermissions(String userId) {
        User user = findUserById(userId);

        Role role = null;
        List<Permission> rolePermissions = Collections.emptyList();

        if (user.getRoleId() != null) {
            role = roleRepository.findById(user.getRoleId())
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle", user.getRoleId()));
            if (role.getPermissionIds() != null && !role.getPermissionIds().isEmpty()) {
                rolePermissions = permissionRepository.findByIdIn(role.getPermissionIds());
            }
        }

        Set<Permission> grantedPermissions = getPermissionsFromIds(user.getGrantedPermissionIds());
        Set<Permission> revokedPermissions = getPermissionsFromIds(user.getRevokedPermissionIds());

        Set<String> effectivePermissionIds = calculateEffectivePermissions(user);
        Set<Permission> effectivePermissions = effectivePermissionIds.isEmpty() ? Collections.emptySet() :
                new HashSet<>(permissionRepository.findByIdIn(new ArrayList<>(effectivePermissionIds)));

        return UserPermissionsResponse.builder()
                .role(role != null ? roleMapper.toDto(role) : null)
                .rolePermissions(rolePermissions.stream()
                        .map(permissionMapper::toDto)
                        .collect(Collectors.toList()))
                .grantedPermissions(grantedPermissions.stream()
                        .map(permissionMapper::toDto)
                        .collect(Collectors.toSet()))
                .revokedPermissions(revokedPermissions.stream()
                        .map(permissionMapper::toDto)
                        .collect(Collectors.toSet()))
                .effectivePermissions(effectivePermissions.stream()
                        .map(permissionMapper::toDto)
                        .collect(Collectors.toSet()))
                .build();
    }

    /**
     * Met à jour les permissions overrides d'un utilisateur
     */
    @Transactional
    public UserPermissionsResponse updateUserPermissions(String userId, UserPermissionsUpdateRequest request) {
        User user = findUserById(userId);

        Set<String> granted = request.getGrantedPermissionIds() != null ? request.getGrantedPermissionIds() : new HashSet<>();
        Set<String> revoked = request.getRevokedPermissionIds() != null ? request.getRevokedPermissionIds() : new HashSet<>();

        // Valider que toutes les permissions existent
        validatePermissions(granted);
        validatePermissions(revoked);

        // Vérifier qu'il n'y a pas de conflit
        Set<String> intersection = new HashSet<>(granted);
        intersection.retainAll(revoked);
        if (!intersection.isEmpty()) {
            throw new BusinessException("Les permissions ne peuvent pas être à la fois accordées et révoquées: " + intersection);
        }

        user.setGrantedPermissionIds(granted);
        user.setRevokedPermissionIds(revoked);
        user.setUpdatedAt(LocalDateTime.now());
        user.setUpdatedBy("SYSTEM");

        userRepository.save(user);
        log.info("✅ Permissions overrides mises à jour pour l'utilisateur: {}", user.getEmail());

        return getUserPermissions(userId);
    }

    /**
     * Vérifie si un utilisateur a une permission spécifique
     */
    public boolean hasPermission(String userId, String permissionName) {
        User user = findUserById(userId);
        Set<String> permissionNames = calculateEffectivePermissionNames(user);
        return permissionNames.contains(permissionName);
    }

    /**
     * Vérifie si un utilisateur peut modifier un autre utilisateur basé sur la hiérarchie
     */
    public boolean canModifyUser(String currentUserId, String targetUserId) {
        User currentUser = findUserById(currentUserId);
        User targetUser = findUserById(targetUserId);

        if (currentUser.getRoleId() == null || targetUser.getRoleId() == null) {
            return false;
        }

        Optional<Role> currentRoleOpt = roleRepository.findById(currentUser.getRoleId());
        Optional<Role> targetRoleOpt = roleRepository.findById(targetUser.getRoleId());

        if (currentRoleOpt.isEmpty() || targetRoleOpt.isEmpty()) {
            return false;
        }

        return currentRoleOpt.get().getHierarchyLevel() > targetRoleOpt.get().getHierarchyLevel();
    }

    private User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", userId));
    }

    private Set<Permission> getPermissionsFromIds(Set<String> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(permissionRepository.findByIdIn(new ArrayList<>(permissionIds)));
    }

    private void validatePermissions(Set<String> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }

        List<Permission> existingPermissions = permissionRepository.findByIdIn(new ArrayList<>(permissionIds));
        Set<String> existingIds = existingPermissions.stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        Set<String> invalidIds = permissionIds.stream()
                .filter(id -> !existingIds.contains(id))
                .collect(Collectors.toSet());

        if (!invalidIds.isEmpty()) {
            throw new BusinessException("Permissions invalides : " + invalidIds);
        }
    }
}
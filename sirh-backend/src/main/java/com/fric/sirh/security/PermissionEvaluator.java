package com.fric.sirh.security;

import com.fric.sirh.model.Permission;
import com.fric.sirh.model.Role;
import com.fric.sirh.model.User;
import com.fric.sirh.repository.PermissionRepository;
import com.fric.sirh.repository.RoleRepository;
import com.fric.sirh.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.List;

@Component("permissionEvaluator")
@RequiredArgsConstructor
public class PermissionEvaluator {

    private static final String ROLE_PREFIX = "ROLE_";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    public boolean hasPermission(Authentication authentication, String permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        User user = resolveAuthenticatedUser(authentication);
        if (user == null) {
            return false;
        }

        Role role = roleRepository.findById(user.getRoleId()).orElse(null);
        if (role == null) {
            return false;
        }

        List<Permission> permissions = permissionRepository.findByIdIn(role.getPermissionIds());
        return permissions.stream().anyMatch(p -> p.getName().equals(permission));
    }

    public boolean hasAnyPermission(Authentication authentication, String... permissions) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        User user = resolveAuthenticatedUser(authentication);
        if (user == null) {
            return false;
        }

        Role role = roleRepository.findById(user.getRoleId()).orElse(null);
        if (role == null) {
            return false;
        }

        List<Permission> userPermissions = permissionRepository.findByIdIn(role.getPermissionIds());

        for (String permission : permissions) {
            if (userPermissions.stream().anyMatch(p -> p.getName().equals(permission))) {
                return true;
            }
        }

        return false;
    }

    public boolean hasRole(Authentication authentication, String roleName) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        User user = resolveAuthenticatedUser(authentication);
        if (user == null) {
            return false;
        }

        Role role = roleRepository.findById(user.getRoleId()).orElse(null);
        if (role == null) {
            return false;
        }

        if (roleName == null) {
            return false;
        }

        // roleName may be provided as "ROLE_ADMIN" or just "ADMIN"
        return role.getName().equals(roleName) || roleName.equals(ROLE_PREFIX + role.getName());
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal == null) {
            return null;
        }

        // If JwtAuthenticationFilter set principal as UserDetails
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails u) {
            // CustomUserDetailsService uses user id as username
            return userRepository.findById(u.getUsername()).orElse(null);
        }

        if (principal instanceof String s) {
            // Could be user id OR email depending on your setup
            return userRepository.findById(s).orElseGet(() -> userRepository.findByEmail(s).orElse(null));
        }

        return null;
    }
}


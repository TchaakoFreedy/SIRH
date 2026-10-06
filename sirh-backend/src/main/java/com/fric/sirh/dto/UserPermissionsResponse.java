package com.fric.sirh.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionsResponse {
    private RoleDto role;
    private List<PermissionDto> rolePermissions;
    private Set<PermissionDto> grantedPermissions;
    private Set<PermissionDto> revokedPermissions;
    private Set<PermissionDto> effectivePermissions;
}
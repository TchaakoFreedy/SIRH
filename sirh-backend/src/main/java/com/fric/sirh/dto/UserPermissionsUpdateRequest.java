package com.fric.sirh.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPermissionsUpdateRequest {
    @NotNull(message = "La liste des permissions accordées est obligatoire")
    private Set<String> grantedPermissionIds;

    @NotNull(message = "La liste des permissions révoquées est obligatoire")
    private Set<String> revokedPermissionIds;
}
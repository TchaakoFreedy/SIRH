package com.fric.sirh.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionsUpdateRequest {
    @NotNull(message = "La liste des permissions est obligatoire")
    private List<String> permissionIds;
}
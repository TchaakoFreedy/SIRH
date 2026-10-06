package com.fric.sirh.dto;

import jakarta.validation.constraints.NotBlank;
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
public class CreateRoleRequest {
    @NotBlank(message = "Le nom du rôle est obligatoire")
    private String name;

    private String description;

    @NotNull(message = "Le niveau hiérarchique est obligatoire")
    private Integer hierarchyLevel;

    private List<String> permissionIds;
    private String visibilityScope;
    private Boolean active;
}
package com.fric.sirh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePermissionRequest {
    @NotBlank(message = "Le nom de la permission est obligatoire")
    private String name;

    private String description;
    private String category;

    @NotNull(message = "Le niveau requis est obligatoire")
    private Integer requiredLevel;

    private Boolean active;
}
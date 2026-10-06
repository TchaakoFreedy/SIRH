package com.fric.sirh.performance.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CriterePerformanceDTO {

    private String id;

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    private String description;

    @NotNull(message = "La note maximale est obligatoire")
    @Min(value = 1, message = "La note maximale doit être supérieure à 0")
    @Max(value = 100, message = "La note maximale ne peut pas dépasser 100")
    private Integer noteMaximale;

    @NotNull(message = "Le coefficient est obligatoire")
    @Min(value = 1, message = "Le coefficient doit être supérieur à 0")
    private Integer coefficient;

    private Boolean actif;

    // ===== CRITERIA TYPES =====
    private String typeCritere; // "GLOBAL" or "SELECTIVE"
    private List<String> employeeIds;
    private List<String> departementIds;
    private Integer ordreAffichage;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;

    public String getTypeLabel() {
        if ("GLOBAL".equals(typeCritere)) return "🌍 Global";
        if ("SELECTIVE".equals(typeCritere)) return "🎯 Sélectif";
        return typeCritere;
    }
}
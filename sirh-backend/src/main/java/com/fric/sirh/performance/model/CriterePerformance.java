package com.fric.sirh.performance.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "criteres_performance")
public class CriterePerformance {

    @Id
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

    @Builder.Default
    private Boolean actif = true;

    // ===== CRITERIA TYPES =====
    // "GLOBAL" - applicable to all employees
    // "SELECTIVE" - only for selected employees (HR chooses)
    @Builder.Default
    private String typeCritere = "GLOBAL";

    // Liste des IDs d'employés (pour les critères sélectifs)
    @Builder.Default
    private List<String> employeeIds = new ArrayList<>();

    // Liste des IDs de départements (pour les critères par département)
    @Builder.Default
    private List<String> departementIds = new ArrayList<>();

    // Ordre d'affichage
    private Integer ordreAffichage;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
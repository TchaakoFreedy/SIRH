package com.fric.sirh.performance.dto;

import com.fric.sirh.performance.enums.PeriodeEvaluation;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRequestDTO {

    @NotNull(message = "L'ID de l'employé est obligatoire")
    private String employeId;

    @NotNull(message = "La période est obligatoire")
    private PeriodeEvaluation periode;

    @NotNull(message = "L'année est obligatoire")
    private Integer annee;

    private String commentaires;

    // Liste des IDs de critères sélectionnés (pour les évaluations individuelles)
    private List<String> critereIds;

    // Notes par critère
    private Map<String, Double> notesParCritere;

    // Type d'évaluation: "GLOBALE" (tous les critères) ou "INDIVIDUELLE" (critères choisis)
    @Builder.Default
    private String typeEvaluation = "GLOBALE";
}
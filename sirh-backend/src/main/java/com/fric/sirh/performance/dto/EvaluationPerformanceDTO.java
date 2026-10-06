package com.fric.sirh.performance.dto;

import com.fric.sirh.performance.enums.PeriodeEvaluation;
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
public class EvaluationPerformanceDTO {

    private String id;

    @NotNull(message = "L'employé est obligatoire")
    private String employeId;
    private String employeNom;

    private String evaluateurId;
    private String evaluateurNom;

    @NotNull(message = "La période est obligatoire")
    private PeriodeEvaluation periode;

    @NotNull(message = "L'année est obligatoire")
    private Integer annee;

    private Integer mois;  // Mois pour les évaluations mensuelles

    private String commentaires;

    private LocalDateTime dateEvaluation;

    @NotNull(message = "Les notes sont obligatoires")
    private List<NoteEvaluationDTO> notes;

    // Nouveau champ optionnel pour spécifier les critères à évaluer
    // Si non fourni, tous les critères applicables (globaux + sélectifs) seront utilisés
    private List<String> critereIds;

    // Champs de lecture
    private List<String> criteresUtilises;
    private String typeEvaluation; // "GLOBALE", "INDIVIDUELLE"

    private Double totalObtenu;
    private Double totalMaximal;
    private Double pourcentage;
    private String mention;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
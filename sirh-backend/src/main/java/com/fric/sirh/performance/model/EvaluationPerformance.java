package com.fric.sirh.performance.model;

import com.fric.sirh.model.Employee;
import com.fric.sirh.model.User;
import com.fric.sirh.performance.enums.PeriodeEvaluation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "evaluations_performance")
public class EvaluationPerformance {

    @Id
    private String id;

    @DBRef
    private Employee employe;

    @DBRef
    private User evaluateur;

    private PeriodeEvaluation periode;

    private Integer annee;

    private String commentaires;

    private Integer mois;

    private LocalDateTime dateEvaluation;

    @Builder.Default
    private List<NoteEvaluation> notes = new ArrayList<>();

    // ===== FIELDS =====
    private String typeEvaluation; // "GLOBALE" or "INDIVIDUELLE"

    // Liste des critères utilisés (pour traçabilité)
    @Builder.Default
    private List<String> criteresUtilises = new ArrayList<>();

    private Double totalObtenu;
    private Double totalMaximal;
    private Double pourcentage;
    private String mention;

    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
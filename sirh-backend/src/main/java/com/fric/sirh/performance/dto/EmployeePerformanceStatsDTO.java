package com.fric.sirh.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeePerformanceStatsDTO {
    private String employeeId;
    private String employeeName;
    private String employeePoste;
    private String departement;

    private int totalEvaluations;
    private int totalEmployes;
    private double moyenneGenerale;
    private String meilleureMention;
    private String derniereMention;

    private EvaluationPerformanceDTO meilleureEvaluation;
    private EvaluationPerformanceDTO pireEvaluation;

    private List<EvolutionPerformanceDTO> evolution;
    private List<MentionDistributionDTO> repartitionMentions;
}
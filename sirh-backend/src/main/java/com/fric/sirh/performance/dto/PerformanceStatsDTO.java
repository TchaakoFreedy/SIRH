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
public class PerformanceStatsDTO {
    private String employeeId;
    private String employeeName;
    private String employeePoste;
    private String departement;
    private Long totalEvaluations;
    private Double moyenneGenerale;
    private String meilleureMention;
    private String derniereMention;
    private List<MentionDistributionDTO> repartitionMentions;
    private List<EvolutionPerformanceDTO> evolution;
}
package com.fric.sirh.performance.dto;

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
public class DashboardPerformanceDTO {
    private Long totalEvaluations;
    private Double moyenneGenerale;
    private ClassementDTO meilleurEmploye;
    private String meilleurDepartement;
    private Map<String, Long> repartitionMentions;
    private List<EvolutionPerformanceDTO> evolutionParMois;
}
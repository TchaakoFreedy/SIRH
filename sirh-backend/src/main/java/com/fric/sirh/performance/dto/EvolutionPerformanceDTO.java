package com.fric.sirh.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolutionPerformanceDTO {
    private String mois;
    private Integer annee;
    private Double moyenne;
    private Long nombreEvaluations;
}
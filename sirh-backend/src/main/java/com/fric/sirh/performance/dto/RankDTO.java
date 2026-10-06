package com.fric.sirh.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RankDTO {
    private String employeeId;
    private String employeeName;
    private Integer rang;
    private Integer totalEmployes;
    private Double scoreTotal;
    private Integer annee;
}
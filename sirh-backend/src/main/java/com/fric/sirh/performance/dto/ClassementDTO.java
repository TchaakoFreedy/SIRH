package com.fric.sirh.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassementDTO {
    private String employeId;
    private String employeNom;
    private String entrepriseId;
    private String entrepriseNom;
    private String departementId;
    private String departementNom;
    private Double scoreTotal;
    private Integer annee;
    private Integer rang;
    private String mention;
}
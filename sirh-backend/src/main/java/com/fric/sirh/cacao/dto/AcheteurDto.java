// src/main/java/com/fric/sirh/cacao/dto/AcheteurDto.java

package com.fric.sirh.cacao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AcheteurDto {
    private String id;
    private String employeeId;
    private String zoneCollecte;
    private String statut;
    private String nomComplet;
    private String telephone;
    private String email;
    private BigDecimal totalAvances;
    private BigDecimal totalValeurLivree;
    private BigDecimal solde;
}
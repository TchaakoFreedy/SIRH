// src/main/java/com/fric/sirh/cacao/dto/SoldeAcheteurDto.java

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
public class SoldeAcheteurDto {
    private String acheteurId;
    private String acheteurNomComplet;
    private BigDecimal totalAvances;
    private BigDecimal totalValeurLivree;
    private BigDecimal totalRemboursements;
    private BigDecimal solde;
}
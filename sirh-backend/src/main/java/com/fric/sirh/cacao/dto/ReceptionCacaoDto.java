// src/main/java/com/fric/sirh/cacao/dto/ReceptionCacaoDto.java

package com.fric.sirh.cacao.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionCacaoDto {
    private String id;
    private String acheteurId;
    private LocalDateTime dateReception;
    private BigDecimal quantiteKg;
    private BigDecimal quantiteRefractee;
    private BigDecimal quantiteNet;
    private String motifRefraction;
    private BigDecimal prixUnitaire;
    private BigDecimal valeurLivree;
    private String qualite;
    private String observations;
    private String numBonReception;
    private String acheteurNomComplet;
    private BigDecimal montantRembourse;
    private String remboursementId;
}
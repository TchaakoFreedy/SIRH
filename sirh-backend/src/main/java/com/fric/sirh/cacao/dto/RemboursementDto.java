// src/main/java/com/fric/sirh/cacao/dto/RemboursementDto.java

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
public class RemboursementDto {
    private String id;
    private String acheteurId;
    private String acheteurNomComplet;
    private String receptionId;
    private String receptionNumBon;
    private BigDecimal quantiteRecue;
    private BigDecimal quantiteAttendue;
    private BigDecimal quantiteSurplus;
    private BigDecimal prixUnitaire;
    private BigDecimal montantRembourse;
    private String motifRemboursement;
    private String modePaiement;
    private String referencePaiement;
    private LocalDateTime dateRemboursement;
    private String statut;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
}
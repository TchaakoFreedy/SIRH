// src/main/java/com/fric/sirh/cacao/dto/AvanceFinanciereDto.java

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
public class AvanceFinanciereDto {
    private String id;
    private String acheteurId;
    private BigDecimal montant;
    private BigDecimal quantiteKg;
    private BigDecimal prixUnitaire;
    private LocalDateTime dateAvance;
    private String motif;
    private String modePaiement;
    private String referencePaiement;
    private String acheteurNomComplet;
}
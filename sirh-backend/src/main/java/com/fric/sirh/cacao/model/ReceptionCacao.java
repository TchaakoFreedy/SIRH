// src/main/java/com/fric/sirh/cacao/model/ReceptionCacao.java

package com.fric.sirh.cacao.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "receptions_cacao")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReceptionCacao {

    @Id
    private String id;

    @Indexed
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

    private BigDecimal montantRembourse;

    private String remboursementId;

    private String createdBy;

    private String updatedBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
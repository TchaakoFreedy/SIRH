// src/main/java/com/fric/sirh/cacao/model/Remboursement.java

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

@Document(collection = "remboursements")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Remboursement {

    @Id
    private String id;

    @Indexed
    private String acheteurId;

    @Indexed
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

    private String updatedBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
// src/main/java/com/fric/sirh/cacao/model/AvanceFinanciere.java

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

@Document(collection = "avances_financieres")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvanceFinanciere {

    @Id
    private String id;

    @Indexed
    private String acheteurId;

    private BigDecimal montant;

    private BigDecimal quantiteKg;

    private BigDecimal prixUnitaire;

    private LocalDateTime dateAvance;

    private String motif;

    private String modePaiement;

    private String referencePaiement;

    private String createdBy;

    private String updatedBy;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
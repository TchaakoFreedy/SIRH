package com.fric.sirh.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Document(collection = "paiements")
public class Paiement {

    @Id
    private String id;

    @Indexed
    private String employeeId;

    private String employeeNom;
    private String employeePrenom;
    private String employeeMatricule;
    private String employeePoste;
    private String employeeDepartement;
    private String employeeTelephone;
    private String employeeEmail;
    private LocalDate employeeDateEmbauche;

    @Indexed
    private TypePaiement type;

    private Double montant;
    private String motif;

    private Integer mois;
    private Integer annee;

    private LocalDateTime datePaiement;

    @Indexed(unique = true, sparse = true)
    private String numeroRecu;

    private Double salaireMensuel;
    private Double totalAvancesMois;
    private Double totalRetenuesMois;
    private Double totalPayeMois;
    private Double montantNetAPayer;
    private Double montantRestantApres;

    private String createdBy;
    private LocalDateTime createdAt;
}
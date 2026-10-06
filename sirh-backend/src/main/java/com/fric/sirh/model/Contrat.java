package com.fric.sirh.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "contrats")
public class Contrat {

    @Id
    private String id;

    @DBRef(lazy = false)
    private Employee employee;

    private String typeContrat;

    private LocalDate dateDebut;

    private LocalDate dateFin;

    private LocalDate dateFinEssai;

    private Integer dureeEssaiMois;

    @Builder.Default
    private String statut = "ACTIF";

    // Remuneration
    private Double salaireBrut;

    private Double salaireNet;

    private Double tauxHoraire;

    private Integer nombreHeuresSemaine;

    // CDD specifique
    private String motifRecours;

    private Integer dureeMois;

    // Stage specifique
    private String etablissement;

    private String tuteurNom;

    private String tuteurEmail;

    private String tuteurTelephone;

    private String objectifsStage;

    private Integer dureeSemaines;

    // Freelance specifique
    private String descriptionPrestation;

    private String modalitesPaiement;

    private Integer dureeMoisPrestation;

    // Renouvellement
    @Builder.Default
    private Boolean estRenouvelable = false;

    @Builder.Default
    private Integer nombreRenouvellements = 0;

    private Integer renouvellementMax;

    @Builder.Default
    private Boolean estRenouvele = false;

    private String contratPrecedentId;

    // Resiliation
    private String motifResiliation;

    private LocalDate dateResiliation;

    @Builder.Default
    private Boolean reminder14Sent = false;

    private LocalDate lastDailyReminderDate;

    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    private String observations;

    private LocalDate createdAt;

    private String createdBy;

    private LocalDate updatedAt;

    private String updatedBy;

    @Builder.Default
    private Integer version = 1;
}
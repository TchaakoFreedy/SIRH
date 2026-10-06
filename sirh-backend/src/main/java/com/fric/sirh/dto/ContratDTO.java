// src/main/java/com/fric/sirh/dto/ContratDTO.java
package com.fric.sirh.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class ContratDTO {

    private String id;
    private String typeContrat;
    private String typeContratLibelle;

    private LocalDate dateDebut;
    private LocalDate dateFin;

    private LocalDate dateFinEssai;
    private Integer dureeEssaiMois;

    private String statut;
    private String statutLibelle;

    private Double salaireBrut;
    private Double salaireNet;
    private Double tauxHoraire;
    private Integer nombreHeuresSemaine;

    private String employeeId;
    private String employeeNom;
    private String employeePrenom;
    private String employeeMatricule;

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

    private Boolean estRenouvelable;
    private Integer nombreRenouvellements;
    private Integer renouvellementMax;
    private Boolean estRenouvele;
    private String contratPrecedentId;

    private String motifResiliation;
    private LocalDate dateResiliation;

    private List<String> imageUrls;

    private String observations;

    private LocalDate createdAt;
    private String createdBy;
    private LocalDate updatedAt;
    private String updatedBy;
}
package com.fric.sirh.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Data
public class UpdateContratRequest {

    private String typeContrat;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateDebut;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFin;

    private String statut;

    // Salaires
    private Double salaireBrut;
    private Double salaireNet;
    private Double tauxHoraire;
    private Integer nombreHeuresSemaine;

    // Période d'essai
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFinEssai;
    private Integer dureeEssaiMois;

    // CDD
    private String motifRecours;
    private Integer dureeMois;

    // Stage
    private String etablissement;
    private String tuteurNom;
    private String tuteurEmail;
    private String tuteurTelephone;
    private String objectifsStage;
    private Integer dureeSemaines;

    // Freelance
    private String descriptionPrestation;
    private String modalitesPaiement;
    private Integer dureeMoisPrestation;

    // Renouvellement
    private Boolean estRenouvelable;
    private Integer renouvellementMax;
    private String contratPrecedentId;

    // Résiliation
    private String motifResiliation;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateResiliation;

    private List<String> imageUrls;

    private String observations;
}
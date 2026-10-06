// src/main/java/com/fric/sirh/dto/CreateContratRequest.java
package com.fric.sirh.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateContratRequest {

    @NotBlank(message = "L'identifiant de l'employé est obligatoire")
    private String employeeId;

    @NotBlank(message = "Le type de contrat est obligatoire")
    private String typeContrat;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    private LocalDate dateFin;

    private String statut;

    // Salaires
    private BigDecimal salaireBrut;
    private BigDecimal salaireNet;
    private BigDecimal tauxHoraire;
    private Integer nombreHeuresSemaine;

    // Période d'essai
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

    private String observations;

    // Flag de remplacement (envoi depuis le front)
    @JsonProperty("replaceActive")
    private Boolean replaceActive = false;

    // Getter/Setter explicites (Lombok les génère automatiquement)
    // Mais on peut ajouter un getter personnalisé pour une valeur par défaut
    public Boolean getReplaceActive() {
        return replaceActive != null ? replaceActive : false;
    }
}
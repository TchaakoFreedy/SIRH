package com.fric.sirh.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EmployeeFullCreationRequest {

    // ==========================================
    // COMPTE UTILISATEUR
    // ==========================================

    @NotBlank(message = "Le nom est obligatoire")
    private String nom;

    @NotBlank(message = "Le prénom est obligatoire")
    private String prenom;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'adresse email est invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est obligatoire")
    private String password;

    private String roleId;

    // ==========================================
    // INFORMATIONS PERSONNELLES
    // ==========================================

    @NotBlank(message = "Le matricule interne est obligatoire")
    private String matricule_interne;

    private String matricule_CNPS;

    private String sexe;

    private Integer nombreEnfantsMoinsDe7Ans;

    private LocalDate date_naissance;

    private String telephone;

    private String numeroContactUrgence;

    private String addresse;

    // ==========================================
    // INFORMATIONS PROFESSIONNELLES
    // ==========================================

    private LocalDate date_embauche;

    private String posteId;

    private String departementId;

    private String entrepriseId;

    // ==========================================
    // CONTRAT
    // ==========================================

    private String typeContrat;

    private LocalDate dateDebutContrat;

    private LocalDate dateFinContrat;

    private LocalDate dateFinEssai;

    private Integer dureeEssaiMois;

    // ==========================================
    // REMUNERATION
    // ==========================================

    private Double salaireBrut;

    private Double salaireNet;

    private Double tauxHoraire;

    private Integer nombreHeuresSemaine;

    // ==========================================
    // CONTRAT CDD
    // ==========================================

    private String motifRecours;

    private Integer dureeMois;

    // ==========================================
    // CONTRAT STAGE
    // ==========================================

    private String etablissement;

    private String tuteurNom;

    private String tuteurEmail;

    private String tuteurTelephone;

    private String objectifsStage;

    private Integer dureeSemaines;

    // ==========================================
    // CONTRAT FREELANCE
    // ==========================================

    private String descriptionPrestation;

    private String modalitesPaiement;

    private Integer dureeMoisPrestation;

    // ==========================================
    // RENOUVELLEMENT
    // ==========================================

    private Boolean estRenouvelable;

    private Integer renouvellementMax;

    // ==========================================
    // OBSERVATIONS
    // ==========================================

    private String observations;

    // ==========================================
    // FICHIERS
    // ==========================================

    private List<String> cniUrls;

    private List<String> certificatUrls;

    private List<String> photoUrls;

    private List<String> contratUrls;
}
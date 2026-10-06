package com.fric.sirh.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Data
public class EmployeeCreationRequest {
    // Infos du Compte Utilisateur
    @NotBlank @Email
    private String email;
    @NotBlank
    private String password;
    private String roleId;

    // Infos Personnelles & Professionnelles de l'employé
    @NotBlank
    private String nom;
    @NotBlank
    private String prenom;
    @NotBlank
    private String matricule_interne;
    private String matricule_CNPS;
    private String sexe;
    private LocalDate date_naissance;
    private Integer telephone;
    private String addresse;
    private LocalDate date_embauche;
    private String poste;
    private String departementId;
}
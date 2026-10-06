package com.fric.sirh.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EmployeeAdminUpdateRequest {

    private String nom;
    private String prenom;
    private String matricule_CNPS;
    private String sexe;
    private LocalDate date_naissance;

    private String telephone;
    private String numeroContactUrgence;   // AJOUTÉ
    private String addresse;

    private LocalDate date_embauche;
    private String entrepriseId;
    private String posteId;
    private String departementId;

    private String statut;
}
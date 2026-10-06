package com.fric.sirh.performance.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeSelectionDTO {
    private String id;
    private String nom;
    private String prenom;
    private String matriculeInterne;
    private String departementId;
    private String departementNom;
    private String statut;
    private boolean selected;

    public String getNomComplet() {
        return (prenom != null ? prenom : "") + " " + (nom != null ? nom : "");
    }
}
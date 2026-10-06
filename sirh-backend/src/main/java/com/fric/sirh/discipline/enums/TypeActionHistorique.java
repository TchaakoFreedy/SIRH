package com.fric.sirh.discipline.enums;

public enum TypeActionHistorique {
    DEMANDE_CREEE("Demande créée"),
    DEMANDE_MODIFIEE("Demande modifiée"),
    EMPLOYE_A_REPONDU("Employé a répondu"),
    REPONSE_VALIDEE("Réponse validée"),
    REPONSE_REJETEE("Réponse rejetée"),
    SANCTION_CREEE("Sanction créée"),
    SANCTION_MODIFIEE("Sanction modifiée"),
    SANCTION_LEVEE("Sanction levée"),
    SANCTION_TERMINEE("Sanction terminée");

    private final String libelle;

    TypeActionHistorique(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
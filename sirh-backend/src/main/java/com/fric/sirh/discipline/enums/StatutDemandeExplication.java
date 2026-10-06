package com.fric.sirh.discipline.enums;

public enum StatutDemandeExplication {
    EN_ATTENTE("En attente"),
    REPONDUE("Répondue"),
    VALIDEE("Validée"),
    REJETEE("Rejetée"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutDemandeExplication(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
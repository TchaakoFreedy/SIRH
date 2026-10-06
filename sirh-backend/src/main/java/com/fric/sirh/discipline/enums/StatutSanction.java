package com.fric.sirh.discipline.enums;

public enum StatutSanction {
    ACTIVE("Active"),
    TERMINEE("Terminée"),
    ANNULEE("Annulée");

    private final String libelle;

    StatutSanction(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
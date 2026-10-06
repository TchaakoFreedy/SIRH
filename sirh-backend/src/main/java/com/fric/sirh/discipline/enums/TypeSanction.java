package com.fric.sirh.discipline.enums;

public enum TypeSanction {
    AVERTISSEMENT_VERBAL("Avertissement verbal"),
    AVERTISSEMENT_ECRIT("Avertissement écrit"),
    BLAME("Blâme"),
    MISE_A_PIED("Mise à pied"),
    SUSPENSION("Suspension"),
    MUTATION_DISCIPLINAIRE("Mutation disciplinaire"),
    LICENCIEMENT("Licenciement"),
    AUTRE("Autre");

    private final String libelle;

    TypeSanction(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }
}
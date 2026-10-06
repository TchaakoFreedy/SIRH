package com.fric.sirh.performance.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PeriodeEvaluation {
    // Périodes mensuelles
    JANVIER("Janvier"),
    FEVRIER("Février"),
    MARS("Mars"),
    AVRIL("Avril"),
    MAI("Mai"),
    JUIN("Juin"),
    JUILLET("Juillet"),
    AOUT("Août"),
    SEPTEMBRE("Septembre"),
    OCTOBRE("Octobre"),
    NOVEMBRE("Novembre"),
    DECEMBRE("Décembre"),
    // Périodes trimestrielles
    TRIMESTRE_1("T1 - Janvier à Mars"),
    TRIMESTRE_2("T2 - Avril à Juin"),
    TRIMESTRE_3("T3 - Juillet à Septembre"),
    TRIMESTRE_4("T4 - Octobre à Décembre"),
    // Périodes semestrielles
    SEMESTRE_1("S1 - Janvier à Juin"),
    SEMESTRE_2("S2 - Juillet à Décembre"),
    // Période annuelle
    ANNUEL("Annuel");

    private final String libelle;

    PeriodeEvaluation(String libelle) {
        this.libelle = libelle;
    }

    @JsonValue
    public String getLibelle() {
        return libelle;
    }

    @JsonCreator
    public static PeriodeEvaluation fromValue(String value) {
        if (value == null) {
            return null;
        }
        try {
            // Essayer de trouver par nom exact
            return PeriodeEvaluation.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Si ce n'est pas une valeur valide, essayer de trouver par libellé
            for (PeriodeEvaluation periode : values()) {
                if (periode.getLibelle().equalsIgnoreCase(value)) {
                    return periode;
                }
            }
            // Si c'est "MENSUEL", retourner JANVIER par défaut
            if ("MENSUEL".equalsIgnoreCase(value)) {
                return JANVIER;
            }
            return null;
        }
    }

    /**
     * Vérifie si la période est mensuelle
     */
    public boolean isMensuel() {
        return this == JANVIER || this == FEVRIER || this == MARS ||
                this == AVRIL || this == MAI || this == JUIN ||
                this == JUILLET || this == AOUT || this == SEPTEMBRE ||
                this == OCTOBRE || this == NOVEMBRE || this == DECEMBRE;
    }

    /**
     * Obtient le numéro du mois pour une période mensuelle
     */
    public Integer getMois() {
        switch (this) {
            case JANVIER: return 1;
            case FEVRIER: return 2;
            case MARS: return 3;
            case AVRIL: return 4;
            case MAI: return 5;
            case JUIN: return 6;
            case JUILLET: return 7;
            case AOUT: return 8;
            case SEPTEMBRE: return 9;
            case OCTOBRE: return 10;
            case NOVEMBRE: return 11;
            case DECEMBRE: return 12;
            default: return null;
        }
    }

    /**
     * Obtient le type de période
     */
    public String getType() {
        if (isMensuel()) return "MENSUEL";
        if (this == TRIMESTRE_1 || this == TRIMESTRE_2 || this == TRIMESTRE_3 || this == TRIMESTRE_4) {
            return "TRIMESTRIEL";
        }
        if (this == SEMESTRE_1 || this == SEMESTRE_2) {
            return "SEMESTRIEL";
        }
        return "ANNUEL";
    }

    @Override
    public String toString() {
        return libelle;
    }
}
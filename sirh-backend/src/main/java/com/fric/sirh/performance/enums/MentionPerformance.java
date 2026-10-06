package com.fric.sirh.performance.enums;

public enum MentionPerformance {
    EXCELLENT("Excellent", 90, 100),
    TRES_BIEN("Très Bien", 80, 89),
    BIEN("Bien", 70, 79),
    ASSEZ_BIEN("Assez Bien", 60, 69),
    MOYEN("Moyen", 50, 59),
    INSUFFISANT("Insuffisant", 0, 49);

    private final String libelle;
    private final int minScore;
    private final int maxScore;

    MentionPerformance(String libelle, int minScore, int maxScore) {
        this.libelle = libelle;
        this.minScore = minScore;
        this.maxScore = maxScore;
    }

    public String getLibelle() {
        return libelle;
    }

    public int getMinScore() {
        return minScore;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public static MentionPerformance fromScore(double score) {
        for (MentionPerformance mention : values()) {
            if (score >= mention.getMinScore() && score <= mention.getMaxScore()) {
                return mention;
            }
        }
        return INSUFFISANT;
    }
}
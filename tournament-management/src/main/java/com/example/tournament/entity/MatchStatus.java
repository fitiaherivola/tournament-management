package com.example.tournament.entity;

public enum MatchStatus {
    /** Une ou deux équipes ne sont pas encore connues (tour suivant d'une élimination directe). */
    EN_ATTENTE("En attente des qualifiés"),
    PROGRAMME("Programmé"),
    TERMINE("Terminé"),
    /** Équipe exemptée de tour (nombre d'équipes qui n'est pas une puissance de deux). */
    EXEMPT("Exempt");

    private final String label;

    MatchStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

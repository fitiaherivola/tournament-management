package com.example.tournament.entity;

public enum TournamentType {
    CHAMPIONNAT_SIMPLE("Championnat simple"),
    CHAMPIONNAT_ALLER_RETOUR("Championnat aller-retour"),
    ELIMINATION_DIRECTE("Élimination directe");

    private final String label;

    TournamentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isChampionship() {
        return this != ELIMINATION_DIRECTE;
    }

    public boolean isDoubleLeg() {
        return this == CHAMPIONNAT_ALLER_RETOUR;
    }
}

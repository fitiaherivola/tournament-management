package com.example.tournament.entity;

public enum MatchPhase {
    CHAMPIONNAT("Championnat"),
    TRENTE_DEUXIEMES("32es de finale"),
    SEIZIEMES("16es de finale"),
    HUITIEMES("Huitièmes de finale"),
    QUARTS("Quarts de finale"),
    DEMI_FINALES("Demi-finales"),
    FINALE("Finale");

    private final String label;

    MatchPhase(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

   
    public static MatchPhase forTeamsInRound(int teamsInRound) {
        return switch (teamsInRound) {
            case 2 -> FINALE;
            case 4 -> DEMI_FINALES;
            case 8 -> QUARTS;
            case 16 -> HUITIEMES;
            case 32 -> SEIZIEMES;
            case 64 -> TRENTE_DEUXIEMES;
            default -> throw new IllegalArgumentException("Taille de tableau non supportée : " + teamsInRound);
        };
    }
}

package com.example.tournament.entity;

import java.util.EnumSet;
import java.util.Set;

/**
 * Cycle de vie d'un tournoi.
 * BROUILLON → INSCRIPTION → PROGRAMME → EN_COURS → TERMINE (ANNULE possible tant que non terminé).
 * Les statuts sont écrits sans accent dans l'API (PROGRAMME, TERMINE, ANNULE).
 */
public enum TournamentStatus {
    BROUILLON("Brouillon"),
    INSCRIPTION("Inscriptions ouvertes"),
    PROGRAMME("Programmé"),
    EN_COURS("En cours"),
    TERMINE("Terminé"),
    ANNULE("Annulé");

    private final String label;

    TournamentStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Un tournoi terminé ou annulé est figé. */
    public boolean isFinal() {
        return this == TERMINE || this == ANNULE;
    }

    public Set<TournamentStatus> allowedTransitions() {
        return switch (this) {
            case BROUILLON -> EnumSet.of(INSCRIPTION, ANNULE);
            case INSCRIPTION -> EnumSet.of(PROGRAMME, ANNULE);
            case PROGRAMME -> EnumSet.of(EN_COURS, INSCRIPTION, ANNULE);
            case EN_COURS -> EnumSet.of(TERMINE, ANNULE);
            case TERMINE, ANNULE -> EnumSet.noneOf(TournamentStatus.class);
        };
    }

    public boolean canTransitionTo(TournamentStatus target) {
        return allowedTransitions().contains(target);
    }
}

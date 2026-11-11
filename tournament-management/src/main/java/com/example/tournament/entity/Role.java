package com.example.tournament.entity;

/**
 * ADMIN : configuration (critères de classement, suppression de tournois)
 * MANAGER : gestion métier (tournois, équipes, calendrier)
 * OPERATOR : opérations quotidiennes (saisie des résultats, démarrage d'un tournoi)
 * USER : consultation
 */
public enum Role {
    ADMIN,
    MANAGER,
    OPERATOR,
    USER
}

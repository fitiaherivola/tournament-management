package com.example.tournament.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Paramètres métier (préfixe « app » dans application.properties). */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        /** Charge des équipes et un tournoi de démonstration au démarrage. */
        @DefaultValue("true") boolean seedDemoData,
        /** Durée retenue pour détecter deux matchs simultanés (chevauchement de créneaux). */
        @DefaultValue("90") int matchDurationMinutes,
        /** Durée d'un créneau par défaut lors de la génération du calendrier. */
        @DefaultValue("120") int defaultSlotMinutes,
        @DefaultValue("Terrain 1") String defaultVenue) {
}

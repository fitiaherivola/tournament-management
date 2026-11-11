package com.example.tournament.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Rencontre d'un tournoi. La contrainte d'unicité empêche la création de deux matchs identiques
 * (même tournoi, même phase, même manche, mêmes équipes à domicile / à l'extérieur).
 */
@Entity
@Table(name = "matches",
        uniqueConstraints = @UniqueConstraint(name = "uk_match_pairing",
                columnNames = {"tournament_id", "phase", "leg", "home_team_id", "away_team_id"}),
        indexes = {
                @Index(name = "idx_match_tournament", columnList = "tournament_id"),
                @Index(name = "idx_match_scheduled", columnList = "scheduledAt")
        })
@Getter
@Setter
@NoArgsConstructor
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchPhase phase;

    /** Journée (championnat) ou numéro de tour (élimination directe), à partir de 1. */
    @Column(nullable = false, name = "round_number")
    private int round;

    /** 1 = match aller (ou unique), 2 = match retour. */
    @Column(nullable = false)
    private int leg = 1;

    /** Position dans le tour, de haut en bas du tableau (élimination directe). */
    private Integer bracketPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "home_team_id")
    private Team homeTeam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "away_team_id")
    private Team awayTeam;

    private LocalDateTime scheduledAt;

    @Column(length = 100)
    private String venue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MatchStatus status = MatchStatus.EN_ATTENTE;

    private Integer homeScore;
    private Integer awayScore;

    /** Tirs au but, uniquement pour départager un nul en élimination directe. */
    private Integer homePenalties;
    private Integer awayPenalties;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_id")
    private Team winner;

    private LocalDateTime resultRecordedAt;

    /** Match du tour suivant dans lequel le vainqueur est qualifié (élimination directe). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "next_match_id")
    private Match nextMatch;

    /** Vrai si le vainqueur occupe la place « domicile » du match suivant. */
    @Column(nullable = false)
    private boolean nextSlotHome = true;

    public boolean hasBothTeams() {
        return homeTeam != null && awayTeam != null;
    }
}

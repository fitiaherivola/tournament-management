package com.example.tournament.entity;

import com.example.tournament.exception.InvalidOperationException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tournaments")
@Getter
@Setter
@NoArgsConstructor
public class Tournament {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 60)
    private String sport;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, length = 150)
    private String location;

    @Column(nullable = false)
    private int maxTeams;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TournamentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TournamentStatus status = TournamentStatus.BROUILLON;

    /** Vrai dès que le calendrier a été généré : empêche toute seconde génération. */
    @Column(nullable = false)
    private boolean matchesGenerated;

    // --- configuration du classement (réservée à l'administrateur) ---

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tournament_ranking_criteria", joinColumns = @JoinColumn(name = "tournament_id"))
    @OrderColumn(name = "position")
    @Enumerated(EnumType.STRING)
    @Column(name = "criterion", length = 30)
    private List<RankingCriterion> rankingCriteria = new ArrayList<>(defaultCriteria());

    @Column(nullable = false)
    private int pointsForWin = 3;

    @Column(nullable = false)
    private int pointsForDraw = 1;

    @Column(nullable = false)
    private int pointsForLoss = 0;

    public static List<RankingCriterion> defaultCriteria() {
        return List.of(RankingCriterion.POINTS, RankingCriterion.GOAL_DIFFERENCE,
                RankingCriterion.GOALS_FOR, RankingCriterion.HEAD_TO_HEAD);
    }

    /** Applique une transition de statut en contrôlant qu'elle est autorisée. */
    public void moveTo(TournamentStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidOperationException("Transition impossible : le tournoi est « " + status.getLabel()
                    + " » et ne peut pas passer à « " + target.getLabel() + " »");
        }
        this.status = target;
    }
}

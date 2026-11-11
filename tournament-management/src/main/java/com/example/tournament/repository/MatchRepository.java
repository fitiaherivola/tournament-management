package com.example.tournament.repository;

import com.example.tournament.entity.Match;
import com.example.tournament.entity.MatchStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long>, JpaSpecificationExecutor<Match> {

    boolean existsByTournamentId(Long tournamentId);

    long countByTournamentId(Long tournamentId);

    long countByTournamentIdAndStatus(Long tournamentId, MatchStatus status);

    long countByTournamentIdAndStatusIn(Long tournamentId, Collection<MatchStatus> statuses);

    List<Match> findByTournamentIdAndStatus(Long tournamentId, MatchStatus status);

    @Query("""
            select m from Match m
            where m.tournament.id = :tournamentId
            order by m.round asc, m.bracketPosition asc, m.scheduledAt asc, m.id asc
            """)
    List<Match> findAllByTournamentOrdered(@Param("tournamentId") Long tournamentId);

    /** Tous les matchs (tous tournois confondus) d'une équipe, pour son historique. */
    @Query("""
            select m from Match m
            where m.homeTeam.id = :teamId or m.awayTeam.id = :teamId
            order by m.scheduledAt asc, m.id asc
            """)
    List<Match> findByTeam(@Param("teamId") Long teamId);

    /**
     * Matchs d'une équipe dont le créneau chevauche l'intervalle ]from, to[ — détecte qu'une équipe
     * serait engagée dans deux matchs simultanés (y compris dans un autre tournoi).
     */
    @Query("""
            select m from Match m
            where m.id <> :excludeId
              and m.scheduledAt > :rangeStart and m.scheduledAt < :rangeEnd
              and m.status <> com.example.tournament.entity.MatchStatus.EXEMPT
              and m.tournament.status <> com.example.tournament.entity.TournamentStatus.ANNULE
              and (m.homeTeam.id = :teamId or m.awayTeam.id = :teamId)
            """)
    List<Match> findTeamConflicts(@Param("teamId") Long teamId,
                                  @Param("rangeStart") LocalDateTime rangeStart,
                                  @Param("rangeEnd") LocalDateTime rangeEnd,
                                  @Param("excludeId") Long excludeId);

    /** Matchs déjà programmés sur le même terrain au même moment. */
    @Query("""
            select m from Match m
            where m.id <> :excludeId
              and m.scheduledAt > :rangeStart and m.scheduledAt < :rangeEnd
              and m.status <> com.example.tournament.entity.MatchStatus.EXEMPT
              and m.tournament.status <> com.example.tournament.entity.TournamentStatus.ANNULE
              and lower(m.venue) = lower(:venue)
            """)
    List<Match> findVenueConflicts(@Param("venue") String venue,
                                   @Param("rangeStart") LocalDateTime rangeStart,
                                   @Param("rangeEnd") LocalDateTime rangeEnd,
                                   @Param("excludeId") Long excludeId);

    @Query("""
            select m from Match m
            where m.status = com.example.tournament.entity.MatchStatus.PROGRAMME
              and m.scheduledAt is not null
              and m.tournament.status in (com.example.tournament.entity.TournamentStatus.PROGRAMME,
                                          com.example.tournament.entity.TournamentStatus.EN_COURS)
            order by m.scheduledAt asc
            """)
    List<Match> findUpcoming(Pageable pageable);

    List<Match> findByStatusOrderByResultRecordedAtDesc(MatchStatus status, Pageable pageable);

    long countByStatus(MatchStatus status);

    @Modifying(flushAutomatically = true)
    @Query("update Match m set m.nextMatch = null where m.tournament.id = :tournamentId")
    int detachNextMatches(@Param("tournamentId") Long tournamentId);

    @Modifying(flushAutomatically = true)
    @Query("delete from Match m where m.tournament.id = :tournamentId")
    int deleteAllByTournament(@Param("tournamentId") Long tournamentId);
}

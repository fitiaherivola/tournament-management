package com.example.tournament.repository;

import com.example.tournament.entity.RegistrationStatus;
import com.example.tournament.entity.TournamentRegistration;
import com.example.tournament.entity.TournamentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<TournamentRegistration, Long> {

    Optional<TournamentRegistration> findByTournamentIdAndTeamId(Long tournamentId, Long teamId);

    long countByTournamentIdAndStatus(Long tournamentId, RegistrationStatus status);

    boolean existsByTeamIdAndStatus(Long teamId, RegistrationStatus status);

    /** Équipes engagées, dans l'ordre d'inscription (sert de tête de série). */
    @Query("""
            select r from TournamentRegistration r join fetch r.team
            where r.tournament.id = :tournamentId
              and r.status = com.example.tournament.entity.RegistrationStatus.REGISTERED
            order by r.registeredAt asc, r.id asc
            """)
    List<TournamentRegistration> findActiveByTournament(@Param("tournamentId") Long tournamentId);

    @Query("""
            select r from TournamentRegistration r join fetch r.team
            where r.tournament.id = :tournamentId
            order by r.registeredAt asc, r.id asc
            """)
    List<TournamentRegistration> findAllByTournament(@Param("tournamentId") Long tournamentId);

    /** Historique d'une équipe : tous ses tournois, du plus récent au plus ancien. */
    @Query("""
            select r from TournamentRegistration r join fetch r.tournament
            where r.team.id = :teamId
            order by r.registeredAt desc
            """)
    List<TournamentRegistration> findHistoryByTeam(@Param("teamId") Long teamId);

    @Query("""
            select count(r) > 0 from TournamentRegistration r
            where r.team.id = :teamId
              and r.status = com.example.tournament.entity.RegistrationStatus.REGISTERED
              and r.tournament.status in :statuses
            """)
    boolean existsActiveInTournamentWithStatus(@Param("teamId") Long teamId,
                                               @Param("statuses") Collection<TournamentStatus> statuses);

    @Modifying(flushAutomatically = true)
    @Query("delete from TournamentRegistration r where r.tournament.id = :tournamentId")
    int deleteAllByTournament(@Param("tournamentId") Long tournamentId);

    @Modifying(flushAutomatically = true)
    @Query("delete from TournamentRegistration r where r.team.id = :teamId")
    int deleteAllByTeam(@Param("teamId") Long teamId);
}

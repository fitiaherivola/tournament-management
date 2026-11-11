package com.example.tournament.repository;

import com.example.tournament.entity.Team;
import com.example.tournament.entity.TeamStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByStatus(TeamStatus status);

    List<Team> findByStatusOrderByNameAsc(TeamStatus status);

    /** Équipes actives pas encore inscrites (ou retirées) dans le tournoi donné. */
    @Query("""
            select t from Team t
            where t.status = com.example.tournament.entity.TeamStatus.ACTIVE
              and t.id not in (
                  select r.team.id from TournamentRegistration r
                  where r.tournament.id = :tournamentId
                    and r.status = com.example.tournament.entity.RegistrationStatus.REGISTERED)
            order by t.name
            """)
    List<Team> findAvailableForTournament(@Param("tournamentId") Long tournamentId);
}

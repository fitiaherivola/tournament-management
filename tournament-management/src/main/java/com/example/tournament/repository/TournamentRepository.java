package com.example.tournament.repository;

import com.example.tournament.entity.Tournament;
import com.example.tournament.entity.TournamentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long>, JpaSpecificationExecutor<Tournament> {

    /** Verrou pessimiste : sérialise les opérations critiques (inscription, génération, résultats). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tournament t where t.id = :id")
    Optional<Tournament> findByIdForUpdate(@Param("id") Long id);

    long countByStatus(TournamentStatus status);

    List<Tournament> findTop5ByStatusInOrderByStartDateAsc(List<TournamentStatus> statuses);
}

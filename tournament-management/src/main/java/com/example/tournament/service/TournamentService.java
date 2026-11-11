package com.example.tournament.service;

import com.example.tournament.dto.request.CreateTournamentRequest;
import com.example.tournament.dto.request.RankingConfigRequest;
import com.example.tournament.dto.request.TournamentFilter;
import com.example.tournament.dto.request.UpdateTournamentRequest;
import com.example.tournament.dto.response.TournamentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TournamentService {

    TournamentResponse create(CreateTournamentRequest request);

    TournamentResponse get(Long id);

    Page<TournamentResponse> search(TournamentFilter filter, Pageable pageable);

    TournamentResponse update(Long id, UpdateTournamentRequest request);

    void delete(Long id);

    /** BROUILLON → INSCRIPTION. */
    TournamentResponse openRegistration(Long id);

    /** PROGRAMME → EN_COURS : les résultats peuvent alors être saisis. */
    TournamentResponse start(Long id);

    TournamentResponse cancel(Long id);

    /** Configuration du classement (critères et barème) — réservée à l'administrateur. */
    TournamentResponse updateRankingConfig(Long id, RankingConfigRequest request);
}

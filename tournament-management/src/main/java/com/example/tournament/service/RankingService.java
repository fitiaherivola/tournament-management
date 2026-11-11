package com.example.tournament.service;

import com.example.tournament.dto.response.StandingResponse;

import java.util.List;

public interface RankingService {

    /** Classement d'un championnat, recalculé à partir des résultats enregistrés. */
    List<StandingResponse> ranking(Long tournamentId);
}

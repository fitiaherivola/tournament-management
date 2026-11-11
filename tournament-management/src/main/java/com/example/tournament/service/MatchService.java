package com.example.tournament.service;

import com.example.tournament.dto.request.MatchFilter;
import com.example.tournament.dto.request.MatchResultRequest;
import com.example.tournament.dto.request.RescheduleMatchRequest;
import com.example.tournament.dto.response.BracketResponse;
import com.example.tournament.dto.response.MatchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface MatchService {

    MatchResponse get(Long id);

    Page<MatchResponse> search(Long tournamentId, MatchFilter filter, Pageable pageable);

    /** Enregistre (ou corrige) un résultat : vainqueur, qualification, clôture du tournoi. */
    MatchResponse recordResult(Long matchId, MatchResultRequest request);

    MatchResponse reschedule(Long matchId, RescheduleMatchRequest request);

    BracketResponse bracket(Long tournamentId);
}

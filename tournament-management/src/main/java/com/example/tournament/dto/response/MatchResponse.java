package com.example.tournament.dto.response;

import com.example.tournament.entity.MatchPhase;
import com.example.tournament.entity.MatchStatus;

import java.time.LocalDateTime;

public record MatchResponse(
        Long id,
        Long tournamentId,
        String tournamentName,
        MatchPhase phase,
        int round,
        int leg,
        Integer bracketPosition,
        TeamSummary homeTeam,
        TeamSummary awayTeam,
        LocalDateTime scheduledAt,
        String venue,
        MatchStatus status,
        Integer homeScore,
        Integer awayScore,
        Integer homePenalties,
        Integer awayPenalties,
        TeamSummary winner,
        Long nextMatchId) {
}

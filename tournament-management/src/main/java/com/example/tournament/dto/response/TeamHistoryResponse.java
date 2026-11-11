package com.example.tournament.dto.response;

import com.example.tournament.entity.RegistrationStatus;
import com.example.tournament.entity.TournamentStatus;

import java.time.LocalDateTime;
import java.util.List;

/** Historique d'une équipe : ses participations aux tournois et tous ses matchs. */
public record TeamHistoryResponse(
        TeamResponse team,
        int matchesPlayed,
        int wins,
        int draws,
        int losses,
        List<Participation> participations,
        List<MatchResponse> matches) {

    public record Participation(Long tournamentId, String tournamentName, TournamentStatus tournamentStatus,
                                RegistrationStatus registrationStatus, LocalDateTime registeredAt) {
    }
}

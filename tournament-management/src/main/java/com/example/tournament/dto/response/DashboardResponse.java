package com.example.tournament.dto.response;

import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long totalTournaments,
        Map<String, Long> tournamentsByStatus,
        long totalTeams,
        long activeTeams,
        long matchesPlayed,
        List<TournamentResponse> activeTournaments,
        List<MatchResponse> upcomingMatches,
        List<MatchResponse> recentResults) {
}

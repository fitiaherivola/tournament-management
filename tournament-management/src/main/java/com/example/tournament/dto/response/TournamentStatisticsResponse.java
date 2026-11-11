package com.example.tournament.dto.response;

import java.util.List;

public record TournamentStatisticsResponse(
        Long tournamentId,
        String tournamentName,
        int teams,
        int totalMatches,
        int playedMatches,
        int remainingMatches,
        int byeMatches,
        double completionRate,
        int totalGoals,
        double averageGoalsPerMatch,
        int draws,
        int homeWins,
        int awayWins,
        MatchResponse highestScoringMatch,
        MatchResponse biggestWin,
        TeamStat bestAttack,
        TeamStat bestDefense,
        TeamSummary champion,
        List<TeamStat> teamStats) {

    /** Statistiques d'une équipe sur les matchs terminés du tournoi. */
    public record TeamStat(Long teamId, String teamName, int played, int wins, int draws, int losses,
                           int goalsFor, int goalsAgainst) {
    }
}

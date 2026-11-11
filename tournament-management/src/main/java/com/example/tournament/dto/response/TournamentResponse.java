package com.example.tournament.dto.response;

import com.example.tournament.entity.RankingCriterion;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.entity.TournamentType;

import java.time.LocalDate;
import java.util.List;

public record TournamentResponse(
        Long id,
        String name,
        String sport,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String location,
        Integer maxTeams,
        TournamentType type,
        TournamentStatus status,
        long registeredTeams,
        boolean matchesGenerated,
        List<RankingCriterion> rankingCriteria,
        int pointsForWin,
        int pointsForDraw,
        int pointsForLoss) {
}

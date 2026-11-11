package com.example.tournament.dto.response;

import java.util.List;

/** Arbre d'élimination directe : un élément par tour, du premier tour à la finale. */
public record BracketResponse(
        Long tournamentId,
        String tournamentName,
        List<BracketRound> rounds,
        TeamSummary champion) {
}

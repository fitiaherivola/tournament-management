package com.example.tournament.service;

import com.example.tournament.dto.request.GenerateMatchesRequest;
import com.example.tournament.dto.response.MatchResponse;

import java.util.List;

public interface MatchGenerationService {

    /**
     * Génère tout le calendrier du tournoi dans une seule transaction (championnat ou arbre
     * d'élimination directe) et fait passer le tournoi à PROGRAMME.
     */
    List<MatchResponse> generate(Long tournamentId, GenerateMatchesRequest request);

    /** Supprime le calendrier tant que la compétition n'a pas commencé. */
    void reset(Long tournamentId);
}

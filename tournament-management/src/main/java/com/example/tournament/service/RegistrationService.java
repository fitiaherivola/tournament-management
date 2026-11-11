package com.example.tournament.service;

import com.example.tournament.dto.response.RegistrationResponse;
import com.example.tournament.dto.response.TeamResponse;

import java.util.List;

public interface RegistrationService {

    RegistrationResponse register(Long tournamentId, Long teamId);

    /** Retire une équipe avant le début de la compétition. */
    void withdraw(Long tournamentId, Long teamId);

    List<RegistrationResponse> list(Long tournamentId, boolean includeWithdrawn);

    /** Équipes actives qui peuvent encore être inscrites au tournoi. */
    List<TeamResponse> availableTeams(Long tournamentId);
}

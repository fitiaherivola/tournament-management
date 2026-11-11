package com.example.tournament.service.impl;

import com.example.tournament.config.AppProperties;
import com.example.tournament.entity.Match;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.entity.Team;
import com.example.tournament.exception.ConflictException;
import com.example.tournament.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Règle métier : une équipe ne peut pas jouer deux matchs simultanés (tous tournois confondus)
 * et un terrain ne peut pas accueillir deux matchs en même temps.
 * Deux matchs se chevauchent si leurs horaires de début sont distants de moins que la durée d'un match.
 */
@Component
@RequiredArgsConstructor
public class MatchConflictChecker {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    private final MatchRepository matchRepository;
    private final AppProperties properties;

    public void assertNoConflict(Match match) {
        if (match.getScheduledAt() == null || match.getStatus() == MatchStatus.EXEMPT) {
            return;
        }
        long duration = properties.matchDurationMinutes();
        LocalDateTime rangeStart = match.getScheduledAt().minusMinutes(duration);
        LocalDateTime rangeEnd = match.getScheduledAt().plusMinutes(duration);
        long excludeId = match.getId() != null ? match.getId() : -1L;

        for (Team team : new Team[] {match.getHomeTeam(), match.getAwayTeam()}) {
            if (team == null) {
                continue;
            }
            List<Match> conflicts = matchRepository.findTeamConflicts(team.getId(), rangeStart, rangeEnd, excludeId);
            if (!conflicts.isEmpty()) {
                Match other = conflicts.get(0);
                throw new ConflictException("L'équipe « " + team.getName() + " » joue déjà un match le "
                        + other.getScheduledAt().format(FORMAT) + " (tournoi « " + other.getTournament().getName()
                        + " ») : deux matchs simultanés sont impossibles");
            }
        }

        if (StringUtils.hasText(match.getVenue())) {
            List<Match> conflicts = matchRepository.findVenueConflicts(match.getVenue(), rangeStart, rangeEnd, excludeId);
            if (!conflicts.isEmpty()) {
                throw new ConflictException("Le terrain « " + match.getVenue() + " » est déjà occupé le "
                        + conflicts.get(0).getScheduledAt().format(FORMAT));
            }
        }
    }
}

package com.example.tournament.service.impl;

import com.example.tournament.dto.response.StandingResponse;
import com.example.tournament.entity.Match;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.entity.Tournament;
import com.example.tournament.exception.InvalidOperationException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.RegistrationRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.service.RankingService;
import com.example.tournament.util.StandingsCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankingServiceImpl implements RankingService {

    private final TournamentRepository tournamentRepository;
    private final RegistrationRepository registrationRepository;
    private final MatchRepository matchRepository;

    @Override
    public List<StandingResponse> ranking(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tournoi", tournamentId));
        if (!tournament.getType().isChampionship()) {
            throw new InvalidOperationException("Le classement n'existe que pour un championnat : "
                    + "consultez l'arbre d'élimination de ce tournoi");
        }

        Map<Long, String> teams = new LinkedHashMap<>();
        registrationRepository.findActiveByTournament(tournamentId)
                .forEach(r -> teams.put(r.getTeam().getId(), r.getTeam().getName()));

        List<StandingsCalculator.Result> results = new ArrayList<>();
        for (Match match : matchRepository.findByTournamentIdAndStatus(tournamentId, MatchStatus.TERMINE)) {
            if (match.hasBothTeams() && match.getHomeScore() != null && match.getAwayScore() != null) {
                results.add(new StandingsCalculator.Result(match.getHomeTeam().getId(), match.getAwayTeam().getId(),
                        match.getHomeScore(), match.getAwayScore()));
            }
        }

        StandingsCalculator.Rules rules = new StandingsCalculator.Rules(
                tournament.getPointsForWin(), tournament.getPointsForDraw(), tournament.getPointsForLoss(),
                List.copyOf(tournament.getRankingCriteria()));
        List<StandingsCalculator.Row> rows = StandingsCalculator.compute(teams, results, rules);

        List<StandingResponse> standings = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            StandingsCalculator.Row row = rows.get(i);
            standings.add(new StandingResponse(i + 1, row.teamId(), row.teamName(), row.played(), row.wins(),
                    row.draws(), row.losses(), row.points(), row.goalsFor(), row.goalsAgainst(),
                    row.goalDifference()));
        }
        return standings;
    }
}

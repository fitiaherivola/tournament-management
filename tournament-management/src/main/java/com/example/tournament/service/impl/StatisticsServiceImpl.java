package com.example.tournament.service.impl;

import com.example.tournament.dto.response.DashboardResponse;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.dto.response.StandingResponse;
import com.example.tournament.dto.response.TeamSummary;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.dto.response.TournamentStatisticsResponse;
import com.example.tournament.dto.response.TournamentStatisticsResponse.TeamStat;
import com.example.tournament.entity.Match;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.entity.RegistrationStatus;
import com.example.tournament.entity.Team;
import com.example.tournament.entity.TeamStatus;
import com.example.tournament.entity.Tournament;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.MatchMapper;
import com.example.tournament.mapper.TeamMapper;
import com.example.tournament.mapper.TournamentMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.RegistrationRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.service.RankingService;
import com.example.tournament.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final RegistrationRepository registrationRepository;
    private final RankingService rankingService;
    private final MatchMapper matchMapper;
    private final TeamMapper teamMapper;
    private final TournamentMapper tournamentMapper;

    /** Accumulateur mutable par équipe. */
    private static final class Acc {
        final Long id;
        final String name;
        int played;
        int wins;
        int draws;
        int losses;
        int goalsFor;
        int goalsAgainst;

        Acc(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        TeamStat toStat() {
            return new TeamStat(id, name, played, wins, draws, losses, goalsFor, goalsAgainst);
        }
    }

    @Override
    public TournamentStatisticsResponse tournamentStatistics(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tournoi", tournamentId));

        Map<Long, Acc> teams = new LinkedHashMap<>();
        registrationRepository.findActiveByTournament(tournamentId)
                .forEach(r -> teams.put(r.getTeam().getId(), new Acc(r.getTeam().getId(), r.getTeam().getName())));

        List<Match> all = matchRepository.findAllByTournamentOrdered(tournamentId);
        int byes = 0;
        int remaining = 0;
        int totalGoals = 0;
        int draws = 0;
        int homeWins = 0;
        int awayWins = 0;
        List<Match> played = new ArrayList<>();

        for (Match match : all) {
            switch (match.getStatus()) {
                case EXEMPT -> byes++;
                case PROGRAMME, EN_ATTENTE -> remaining++;
                case TERMINE -> played.add(match);
            }
        }

        for (Match match : played) {
            int hs = match.getHomeScore();
            int as = match.getAwayScore();
            totalGoals += hs + as;
            if (hs > as) {
                homeWins++;
            } else if (hs < as) {
                awayWins++;
            } else {
                draws++;
            }
            Acc home = teams.get(match.getHomeTeam().getId());
            Acc away = teams.get(match.getAwayTeam().getId());
            if (home == null || away == null) {
                continue;
            }
            home.played++;
            away.played++;
            home.goalsFor += hs;
            home.goalsAgainst += as;
            away.goalsFor += as;
            away.goalsAgainst += hs;
            if (match.getWinner() == null) {
                home.draws++;
                away.draws++;
            } else if (match.getWinner().getId().equals(home.id)) {
                home.wins++;
                away.losses++;
            } else {
                away.wins++;
                home.losses++;
            }
        }

        int total = all.size() - byes;
        double completion = total == 0 ? 0 : round1(100.0 * played.size() / total);
        double average = played.isEmpty() ? 0 : round2((double) totalGoals / played.size());

        MatchResponse highest = played.stream()
                .max(Comparator.comparingInt((Match m) -> m.getHomeScore() + m.getAwayScore()))
                .map(matchMapper::toResponse).orElse(null);
        MatchResponse biggest = played.stream()
                .filter(m -> !m.getHomeScore().equals(m.getAwayScore()))
                .max(Comparator.comparingInt((Match m) -> Math.abs(m.getHomeScore() - m.getAwayScore())))
                .map(matchMapper::toResponse).orElse(null);

        List<Acc> withGames = teams.values().stream().filter(a -> a.played > 0).toList();
        TeamStat bestAttack = withGames.stream()
                .max(Comparator.comparingInt((Acc a) -> a.goalsFor).thenComparing(a -> a.name, Comparator.reverseOrder()))
                .map(Acc::toStat).orElse(null);
        TeamStat bestDefense = withGames.stream()
                .min(Comparator.comparingInt((Acc a) -> a.goalsAgainst).thenComparing(a -> a.name))
                .map(Acc::toStat).orElse(null);

        return new TournamentStatisticsResponse(tournamentId, tournament.getName(), teams.size(), total,
                played.size(), remaining, byes, completion, totalGoals, average, draws, homeWins, awayWins,
                highest, biggest, bestAttack, bestDefense, champion(tournament, all),
                teams.values().stream().map(Acc::toStat).toList());
    }

    @Override
    public DashboardResponse dashboard() {
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TournamentStatus status : TournamentStatus.values()) {
            byStatus.put(status.name(), tournamentRepository.countByStatus(status));
        }
        List<TournamentResponse> active = tournamentRepository
                .findTop5ByStatusInOrderByStartDateAsc(List.of(
                        TournamentStatus.INSCRIPTION, TournamentStatus.PROGRAMME, TournamentStatus.EN_COURS))
                .stream()
                .map(t -> tournamentMapper.toResponse(t,
                        registrationRepository.countByTournamentIdAndStatus(t.getId(), RegistrationStatus.REGISTERED)))
                .toList();
        List<MatchResponse> upcoming = matchRepository.findUpcoming(PageRequest.of(0, 8)).stream()
                .map(matchMapper::toResponse).toList();
        List<MatchResponse> recent = matchRepository
                .findByStatusOrderByResultRecordedAtDesc(MatchStatus.TERMINE, PageRequest.of(0, 8)).stream()
                .map(matchMapper::toResponse).toList();

        return new DashboardResponse(tournamentRepository.count(), byStatus, teamRepository.count(),
                teamRepository.countByStatus(TeamStatus.ACTIVE), matchRepository.countByStatus(MatchStatus.TERMINE),
                active, upcoming, recent);
    }

    /** Vainqueur de la finale (élimination directe) ou premier du classement (championnat terminé). */
    private TeamSummary champion(Tournament tournament, List<Match> matches) {
        if (tournament.getType().isChampionship()) {
            if (tournament.getStatus() != TournamentStatus.TERMINE) {
                return null;
            }
            List<StandingResponse> ranking = rankingService.ranking(tournament.getId());
            return ranking.isEmpty() ? null : new TeamSummary(ranking.get(0).teamId(), ranking.get(0).teamName());
        }
        if (matches.isEmpty()) {
            return null;
        }
        Match last = matches.get(matches.size() - 1);
        Team winner = last.getNextMatch() == null && last.getStatus() == MatchStatus.TERMINE ? last.getWinner() : null;
        return teamMapper.toSummary(winner);
    }

    private static double round1(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}

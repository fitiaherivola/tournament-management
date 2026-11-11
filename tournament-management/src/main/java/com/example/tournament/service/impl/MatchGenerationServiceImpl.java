package com.example.tournament.service.impl;

import com.example.tournament.config.AppProperties;
import com.example.tournament.dto.request.GenerateMatchesRequest;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.entity.Match;
import com.example.tournament.entity.MatchPhase;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.entity.Team;
import com.example.tournament.entity.Tournament;
import com.example.tournament.entity.TournamentRegistration;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.exception.ConflictException;
import com.example.tournament.exception.InvalidOperationException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.MatchMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.RegistrationRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.service.MatchGenerationService;
import com.example.tournament.util.KnockoutBracketPlanner;
import com.example.tournament.util.RoundRobinPlanner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Moteur de génération du calendrier. Toute la génération (création des matchs, programmation,
 * contrôle des conflits, changement de statut du tournoi) s'exécute dans UNE transaction :
 * en cas d'erreur, rien n'est enregistré.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchGenerationServiceImpl implements MatchGenerationService {

    private final TournamentRepository tournamentRepository;
    private final RegistrationRepository registrationRepository;
    private final MatchRepository matchRepository;
    private final MatchConflictChecker conflictChecker;
    private final MatchMapper matchMapper;
    private final AppProperties properties;

    @Override
    @Transactional
    public List<MatchResponse> generate(Long tournamentId, GenerateMatchesRequest request) {
        Tournament tournament = tournamentRepository.findByIdForUpdate(tournamentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tournoi", tournamentId));

        if (tournament.isMatchesGenerated() || matchRepository.existsByTournamentId(tournamentId)) {
            throw new ConflictException("Les matchs de ce tournoi ont déjà été générés : "
                    + "ils ne peuvent pas l'être une seconde fois");
        }
        if (tournament.getStatus() != TournamentStatus.INSCRIPTION) {
            throw new InvalidOperationException("Le calendrier ne peut être généré que pendant la phase d'inscription "
                    + "(statut actuel : « " + tournament.getStatus().getLabel() + " »)");
        }

        List<Team> teams = registrationRepository.findActiveByTournament(tournamentId).stream()
                .map(TournamentRegistration::getTeam)
                .collect(Collectors.toCollection(ArrayList::new));
        if (teams.size() < 2) {
            throw new InvalidOperationException("Il faut au moins 2 équipes inscrites pour générer les matchs "
                    + "(actuellement : " + teams.size() + ")");
        }

        GenerateMatchesRequest params = request != null ? request : new GenerateMatchesRequest();
        int slotMinutes = params.getSlotMinutes() != null ? params.getSlotMinutes() : properties.defaultSlotMinutes();
        if (slotMinutes < properties.matchDurationMinutes()) {
            throw new InvalidOperationException("Le créneau (" + slotMinutes + " min) doit être au moins égal à la "
                    + "durée d'un match (" + properties.matchDurationMinutes() + " min)");
        }
        int intervalDays = params.getRoundIntervalDays() != null ? params.getRoundIntervalDays() : 1;
        LocalDateTime start = params.getStartDateTime() != null
                ? params.getStartDateTime() : tournament.getStartDate().atTime(10, 0);
        List<String> venues = params.getVenues() == null ? new ArrayList<>() : params.getVenues().stream()
                .filter(StringUtils::hasText).map(String::trim).distinct().collect(Collectors.toCollection(ArrayList::new));
        if (venues.isEmpty()) {
            venues.add(properties.defaultVenue());
        }
        if (params.isShuffle()) {
            Collections.shuffle(teams);
        }

        List<Match> matches = tournament.getType().isChampionship()
                ? buildChampionship(tournament, teams)
                : buildKnockout(tournament, teams);
        schedule(matches, start, slotMinutes, intervalDays, venues);
        matchRepository.saveAll(matches);

        // une équipe ne peut pas jouer deux matchs simultanés (y compris dans un autre tournoi)
        matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.PROGRAMME && m.hasBothTeams())
                .forEach(conflictChecker::assertNoConflict);

        tournament.setMatchesGenerated(true);
        tournament.moveTo(TournamentStatus.PROGRAMME);

        return matchRepository.findAllByTournamentOrdered(tournamentId).stream().map(matchMapper::toResponse).toList();
    }

    @Override
    @Transactional
    public void reset(Long tournamentId) {
        Tournament tournament = tournamentRepository.findByIdForUpdate(tournamentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tournoi", tournamentId));
        if (!tournament.isMatchesGenerated() && !matchRepository.existsByTournamentId(tournamentId)) {
            throw new InvalidOperationException("Ce tournoi n'a pas de calendrier à supprimer");
        }
        if (tournament.getStatus() != TournamentStatus.PROGRAMME) {
            throw new InvalidOperationException("Le calendrier ne peut être supprimé que tant que la compétition "
                    + "n'a pas commencé (statut actuel : « " + tournament.getStatus().getLabel() + " »)");
        }
        matchRepository.detachNextMatches(tournamentId);
        matchRepository.deleteAllByTournament(tournamentId);
        tournament.setMatchesGenerated(false);
        tournament.moveTo(TournamentStatus.INSCRIPTION);
    }

    // ------------------------------------------------------------------ championnat

    private List<Match> buildChampionship(Tournament tournament, List<Team> teams) {
        Map<Long, Team> byId = new HashMap<>();
        teams.forEach(t -> byId.put(t.getId(), t));
        List<RoundRobinPlanner.Pairing> plan = RoundRobinPlanner.plan(
                teams.stream().map(Team::getId).toList(), tournament.getType().isDoubleLeg());

        List<Match> matches = new ArrayList<>();
        for (RoundRobinPlanner.Pairing pairing : plan) {
            Match match = new Match();
            match.setTournament(tournament);
            match.setPhase(MatchPhase.CHAMPIONNAT);
            match.setRound(pairing.round());
            match.setLeg(pairing.leg());
            match.setHomeTeam(byId.get(pairing.homeId()));
            match.setAwayTeam(byId.get(pairing.awayId()));
            match.setStatus(MatchStatus.PROGRAMME);
            matches.add(match);
        }
        return matches;
    }

    // ------------------------------------------------------------ élimination directe

    /**
     * Construit tout l'arbre. Le nombre de tours dépend du nombre d'équipes ; si ce nombre n'est
     * pas une puissance de deux, les mieux classées (ordre d'inscription) sont exemptées du premier tour.
     * Les matchs sont retournés de la finale vers le premier tour : chaque match est donc
     * enregistré après le match qu'il alimente.
     */
    private List<Match> buildKnockout(Tournament tournament, List<Team> teams) {
        int teamCount = teams.size();
        int size = KnockoutBracketPlanner.bracketSize(teamCount);
        int rounds = KnockoutBracketPlanner.roundCount(teamCount);

        Map<Integer, List<Match>> byRound = new TreeMap<>();
        List<Match> ordered = new ArrayList<>();
        for (int round = rounds; round >= 1; round--) {
            int count = size >> round;
            List<Match> current = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                Match match = new Match();
                match.setTournament(tournament);
                match.setRound(round);
                match.setPhase(MatchPhase.forTeamsInRound(size >> (round - 1)));
                match.setBracketPosition(i);
                match.setStatus(MatchStatus.EN_ATTENTE);
                if (round < rounds) {
                    match.setNextMatch(byRound.get(round + 1).get(i / 2));
                    match.setNextSlotHome(i % 2 == 0);
                }
                current.add(match);
                ordered.add(match);
            }
            byRound.put(round, current);
        }

        List<Integer[]> pairs = KnockoutBracketPlanner.firstRoundPairs(teamCount);
        List<Match> firstRound = byRound.get(1);
        for (int i = 0; i < firstRound.size(); i++) {
            Match match = firstRound.get(i);
            Integer[] pair = pairs.get(i);
            Team a = pair[0] != null ? teams.get(pair[0] - 1) : null;
            Team b = pair[1] != null ? teams.get(pair[1] - 1) : null;
            if (a != null && b != null) {
                match.setHomeTeam(a);
                match.setAwayTeam(b);
                match.setStatus(MatchStatus.PROGRAMME);
            } else {
                // exempt : l'équipe seule est qualifiée d'office pour le tour suivant
                Team solo = a != null ? a : b;
                match.setHomeTeam(solo);
                match.setWinner(solo);
                match.setStatus(MatchStatus.EXEMPT);
                Match next = match.getNextMatch();
                if (next != null) {
                    if (match.isNextSlotHome()) {
                        next.setHomeTeam(solo);
                    } else {
                        next.setAwayTeam(solo);
                    }
                }
            }
        }
        // un match du 2e tour dont les deux équipes sont déjà connues (deux exempts) est programmé
        for (int round = 2; round <= rounds; round++) {
            for (Match match : byRound.get(round)) {
                if (match.hasBothTeams()) {
                    match.setStatus(MatchStatus.PROGRAMME);
                }
            }
        }
        return ordered;
    }

    // ------------------------------------------------------------------ programmation

    /**
     * Date, heure et terrain de chaque match : un tour par « jour » (selon l'intervalle demandé),
     * les matchs d'un même tour étant répartis sur les terrains puis sur des créneaux successifs.
     * Les exempts ne sont pas programmés.
     */
    private void schedule(List<Match> matches, LocalDateTime start, int slotMinutes, int intervalDays,
                          List<String> venues) {
        Map<Integer, List<Match>> byRound = matches.stream()
                .filter(m -> m.getStatus() != MatchStatus.EXEMPT)
                .collect(Collectors.groupingBy(Match::getRound, TreeMap::new, Collectors.toList()));

        int roundIndex = 0;
        for (List<Match> roundMatches : byRound.values()) {
            roundMatches.sort(Comparator
                    .comparing((Match m) -> m.getBracketPosition() == null ? 0 : m.getBracketPosition())
                    .thenComparing(m -> m.getHomeTeam() == null ? 0L : m.getHomeTeam().getId()));
            LocalDateTime roundStart = start.plusDays((long) roundIndex * intervalDays);
            for (int j = 0; j < roundMatches.size(); j++) {
                Match match = roundMatches.get(j);
                match.setVenue(venues.get(j % venues.size()));
                match.setScheduledAt(roundStart.plusMinutes((long) (j / venues.size()) * slotMinutes));
            }
            roundIndex++;
        }
    }
}

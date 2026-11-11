package com.example.tournament.service.impl;

import com.example.tournament.dto.request.MatchFilter;
import com.example.tournament.dto.request.MatchResultRequest;
import com.example.tournament.dto.request.RescheduleMatchRequest;
import com.example.tournament.dto.response.BracketResponse;
import com.example.tournament.dto.response.BracketRound;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.entity.Match;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.entity.Team;
import com.example.tournament.entity.Tournament;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.entity.TournamentType;
import com.example.tournament.exception.ConflictException;
import com.example.tournament.exception.InvalidOperationException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.MatchMapper;
import com.example.tournament.mapper.TeamMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.MatchSpecifications;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.service.MatchService;
import com.example.tournament.util.SortGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchServiceImpl implements MatchService {

    private static final Set<String> SORTABLE = Set.of("id", "round", "scheduledAt", "phase", "status", "venue");

    private final MatchRepository matchRepository;
    private final TournamentRepository tournamentRepository;
    private final MatchConflictChecker conflictChecker;
    private final MatchMapper matchMapper;
    private final TeamMapper teamMapper;

    @Override
    public MatchResponse get(Long id) {
        return matchMapper.toResponse(find(id));
    }

    @Override
    public Page<MatchResponse> search(Long tournamentId, MatchFilter filter, Pageable pageable) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw ResourceNotFoundException.of("Tournoi", tournamentId);
        }
        Pageable effective = SortGuard.sanitize(pageable, SORTABLE, Sort.by("round", "scheduledAt", "id"));
        return matchRepository.findAll(MatchSpecifications.forTournament(tournamentId, filter), effective)
                .map(matchMapper::toResponse);
    }

    /**
     * Enregistre un résultat, détermine le vainqueur, qualifie l'équipe suivante (élimination directe)
     * et clôture le tournoi si tous les matchs sont joués — le tout dans une seule transaction.
     * Le classement n'étant jamais stocké mais recalculé depuis les résultats, il est automatiquement
     * à jour, y compris après la correction d'un résultat.
     */
    @Override
    @Transactional
    public MatchResponse recordResult(Long matchId, MatchResultRequest request) {
        Match match = find(matchId);
        Tournament tournament = tournamentRepository.findByIdForUpdate(match.getTournament().getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Tournoi", match.getTournament().getId()));

        ensureResultAllowed(tournament, match);

        boolean knockout = tournament.getType() == TournamentType.ELIMINATION_DIRECTE;
        int homeScore = request.getHomeScore();
        int awayScore = request.getAwayScore();
        Integer homePenalties = null;
        Integer awayPenalties = null;
        Team winner;

        if (homeScore != awayScore) {
            winner = homeScore > awayScore ? match.getHomeTeam() : match.getAwayTeam();
        } else if (!knockout) {
            winner = null; // match nul valide en championnat
        } else {
            if (request.getHomePenalties() == null || request.getAwayPenalties() == null) {
                throw new InvalidOperationException("Un match nul est impossible en élimination directe : "
                        + "renseignez les tirs au but pour désigner le vainqueur");
            }
            if (request.getHomePenalties().equals(request.getAwayPenalties())) {
                throw new InvalidOperationException("Les tirs au but doivent désigner un vainqueur");
            }
            homePenalties = request.getHomePenalties();
            awayPenalties = request.getAwayPenalties();
            winner = homePenalties > awayPenalties ? match.getHomeTeam() : match.getAwayTeam();
        }

        boolean wasFinished = match.getStatus() == MatchStatus.TERMINE;
        Team previousWinner = match.getWinner();
        boolean winnerChanged = !wasFinished
                || (previousWinner == null ? winner != null
                : winner == null || !previousWinner.getId().equals(winner.getId()));

        Match next = match.getNextMatch();
        if (knockout && wasFinished && winnerChanged && next != null && next.getStatus() == MatchStatus.TERMINE) {
            throw new ConflictException("Le vainqueur ne peut plus être modifié : "
                    + "le match suivant a déjà été joué");
        }

        match.setHomeScore(homeScore);
        match.setAwayScore(awayScore);
        match.setHomePenalties(homePenalties);
        match.setAwayPenalties(awayPenalties);
        match.setWinner(winner);
        match.setStatus(MatchStatus.TERMINE);
        match.setResultRecordedAt(LocalDateTime.now());

        if (knockout && next != null && winnerChanged) {
            placeInNextMatch(match, winner);
        }

        long remaining = matchRepository.countByTournamentIdAndStatusIn(tournament.getId(),
                List.of(MatchStatus.PROGRAMME, MatchStatus.EN_ATTENTE));
        if (remaining == 0 && tournament.getStatus() == TournamentStatus.EN_COURS) {
            tournament.moveTo(TournamentStatus.TERMINE);
        }
        return matchMapper.toResponse(match);
    }

    @Override
    @Transactional
    public MatchResponse reschedule(Long matchId, RescheduleMatchRequest request) {
        Match match = find(matchId);
        Tournament tournament = tournamentRepository.findByIdForUpdate(match.getTournament().getId())
                .orElseThrow(() -> ResourceNotFoundException.of("Tournoi", match.getTournament().getId()));

        if (tournament.getStatus() != TournamentStatus.PROGRAMME && tournament.getStatus() != TournamentStatus.EN_COURS) {
            throw new InvalidOperationException("Le calendrier d'un tournoi « " + tournament.getStatus().getLabel()
                    + " » ne peut pas être modifié");
        }
        if (match.getStatus() == MatchStatus.TERMINE || match.getStatus() == MatchStatus.EXEMPT) {
            throw new InvalidOperationException("Un match terminé ou exempt ne peut pas être reprogrammé");
        }
        match.setScheduledAt(request.getScheduledAt());
        match.setVenue(request.getVenue().trim());
        conflictChecker.assertNoConflict(match);
        return matchMapper.toResponse(match);
    }

    @Override
    public BracketResponse bracket(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Tournoi", tournamentId));
        if (tournament.getType().isChampionship()) {
            throw new InvalidOperationException(
                    "L'arbre d'élimination n'existe que pour un tournoi à élimination directe");
        }
        List<Match> matches = matchRepository.findAllByTournamentOrdered(tournamentId);
        Map<Integer, List<MatchResponse>> byRound = new LinkedHashMap<>();
        Map<Integer, Match> firstOfRound = new LinkedHashMap<>();
        for (Match match : matches) {
            byRound.computeIfAbsent(match.getRound(), r -> new ArrayList<>()).add(matchMapper.toResponse(match));
            firstOfRound.putIfAbsent(match.getRound(), match);
        }
        List<BracketRound> rounds = new ArrayList<>();
        byRound.forEach((round, list) -> {
            Match first = firstOfRound.get(round);
            rounds.add(new BracketRound(round, first.getPhase(), first.getPhase().getLabel(), list));
        });

        Team champion = null;
        if (!matches.isEmpty()) {
            Match last = matches.get(matches.size() - 1);
            if (last.getNextMatch() == null && last.getStatus() == MatchStatus.TERMINE) {
                champion = last.getWinner();
            }
        }
        return new BracketResponse(tournamentId, tournament.getName(), rounds, teamMapper.toSummary(champion));
    }

    // ------------------------------------------------------------------ règles

    private void ensureResultAllowed(Tournament tournament, Match match) {
        switch (tournament.getStatus()) {
            case EN_COURS -> { /* saisie autorisée */ }
            case TERMINE -> throw new InvalidOperationException(
                    "Le tournoi est terminé : plus aucun résultat ne peut être modifié");
            case ANNULE -> throw new InvalidOperationException("Le tournoi est annulé : aucun résultat ne peut être saisi");
            default -> throw new InvalidOperationException(
                    "Le tournoi n'a pas encore démarré : démarrez-le avant de saisir des résultats");
        }
        if (match.getStatus() == MatchStatus.EXEMPT) {
            throw new InvalidOperationException("Ce match est un exempt : il n'y a aucun résultat à saisir");
        }
        if (!match.hasBothTeams()) {
            throw new InvalidOperationException("Les deux équipes ne sont pas encore connues : "
                    + "le match précédent doit d'abord avoir un résultat valide");
        }
    }

    /** Qualifie (ou re-qualifie après correction) l'équipe gagnante dans le match suivant du tableau. */
    private void placeInNextMatch(Match match, Team winner) {
        Match next = match.getNextMatch();
        if (match.isNextSlotHome()) {
            next.setHomeTeam(winner);
        } else {
            next.setAwayTeam(winner);
        }
        if (next.hasBothTeams()) {
            if (next.getStatus() == MatchStatus.EN_ATTENTE) {
                next.setStatus(MatchStatus.PROGRAMME);
            }
            conflictChecker.assertNoConflict(next);
        }
    }

    private Match find(Long id) {
        return matchRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Match", id));
    }
}

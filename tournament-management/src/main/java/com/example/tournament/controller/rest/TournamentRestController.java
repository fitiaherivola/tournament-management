package com.example.tournament.controller.rest;

import com.example.tournament.dto.request.CreateTournamentRequest;
import com.example.tournament.dto.request.GenerateMatchesRequest;
import com.example.tournament.dto.request.MatchFilter;
import com.example.tournament.dto.request.RankingConfigRequest;
import com.example.tournament.dto.request.RegisterTeamRequest;
import com.example.tournament.dto.request.TournamentFilter;
import com.example.tournament.dto.request.UpdateTournamentRequest;
import com.example.tournament.dto.response.BracketResponse;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.dto.response.PageResponse;
import com.example.tournament.dto.response.RegistrationResponse;
import com.example.tournament.dto.response.StandingResponse;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.dto.response.TournamentStatisticsResponse;
import com.example.tournament.service.MatchGenerationService;
import com.example.tournament.service.MatchService;
import com.example.tournament.service.RankingService;
import com.example.tournament.service.RegistrationService;
import com.example.tournament.service.StatisticsService;
import com.example.tournament.service.TournamentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * API REST des tournois. Ce contrôleur ne contient aucune règle métier : il traduit HTTP vers
 * les services, exactement les mêmes que ceux utilisés par l'interface Thymeleaf.
 */
@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
public class TournamentRestController {

    private final TournamentService tournamentService;
    private final RegistrationService registrationService;
    private final MatchGenerationService generationService;
    private final MatchService matchService;
    private final RankingService rankingService;
    private final StatisticsService statisticsService;

    // ------------------------------------------------------------------ tournois

    @PostMapping
    public ResponseEntity<TournamentResponse> create(@Valid @RequestBody CreateTournamentRequest request) {
        TournamentResponse created = tournamentService.create(request);
        return ResponseEntity.created(location(created.id())).body(created);
    }

    /** Exemple : GET /api/tournaments?page=0&size=10&sort=startDate,desc&status=EN_COURS&search=ligue */
    @GetMapping
    public PageResponse<TournamentResponse> list(TournamentFilter filter, Pageable pageable) {
        return PageResponse.from(tournamentService.search(filter, pageable));
    }

    @GetMapping("/{id}")
    public TournamentResponse get(@PathVariable Long id) {
        return tournamentService.get(id);
    }

    @PutMapping("/{id}")
    public TournamentResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTournamentRequest request) {
        return tournamentService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tournamentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------ cycle de vie

    @PostMapping("/{id}/open-registration")
    public TournamentResponse openRegistration(@PathVariable Long id) {
        return tournamentService.openRegistration(id);
    }

    @PostMapping("/{id}/start")
    public TournamentResponse start(@PathVariable Long id) {
        return tournamentService.start(id);
    }

    @PostMapping("/{id}/cancel")
    public TournamentResponse cancel(@PathVariable Long id) {
        return tournamentService.cancel(id);
    }

    /** Réservé à l'administrateur : critères de classement et barème de points. */
    @PutMapping("/{id}/ranking-config")
    public TournamentResponse updateRankingConfig(@PathVariable Long id,
                                                  @Valid @RequestBody RankingConfigRequest request) {
        return tournamentService.updateRankingConfig(id, request);
    }

    // ------------------------------------------------------------------ équipes inscrites

    @PostMapping("/{id}/teams")
    public ResponseEntity<RegistrationResponse> registerTeam(@PathVariable Long id,
                                                             @Valid @RequestBody RegisterTeamRequest request) {
        RegistrationResponse registration = registrationService.register(id, request.getTeamId());
        return ResponseEntity.status(HttpStatus.CREATED).body(registration);
    }

    @DeleteMapping("/{id}/teams/{teamId}")
    public ResponseEntity<Void> withdrawTeam(@PathVariable Long id, @PathVariable Long teamId) {
        registrationService.withdraw(id, teamId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/teams")
    public List<RegistrationResponse> registrations(@PathVariable Long id,
                                                    @RequestParam(defaultValue = "false") boolean includeWithdrawn) {
        return registrationService.list(id, includeWithdrawn);
    }

    @GetMapping("/{id}/available-teams")
    public List<TeamResponse> availableTeams(@PathVariable Long id) {
        return registrationService.availableTeams(id);
    }

    // ------------------------------------------------------------------ calendrier et matchs

    /** Corps facultatif : sans corps, les valeurs par défaut sont utilisées. */
    @PostMapping("/{id}/generate-matches")
    public ResponseEntity<List<MatchResponse>> generateMatches(
            @PathVariable Long id, @Valid @RequestBody(required = false) GenerateMatchesRequest request) {
        List<MatchResponse> matches = generationService.generate(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(matches);
    }

    @DeleteMapping("/{id}/matches")
    public ResponseEntity<Void> resetMatches(@PathVariable Long id) {
        generationService.reset(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/matches")
    public PageResponse<MatchResponse> matches(@PathVariable Long id, MatchFilter filter, Pageable pageable) {
        return PageResponse.from(matchService.search(id, filter, pageable));
    }

    @GetMapping("/{id}/ranking")
    public List<StandingResponse> ranking(@PathVariable Long id) {
        return rankingService.ranking(id);
    }

    @GetMapping("/{id}/bracket")
    public BracketResponse bracket(@PathVariable Long id) {
        return matchService.bracket(id);
    }

    @GetMapping("/{id}/statistics")
    public TournamentStatisticsResponse statistics(@PathVariable Long id) {
        return statisticsService.tournamentStatistics(id);
    }

    private URI location(Long id) {
        return ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(id).toUri();
    }
}

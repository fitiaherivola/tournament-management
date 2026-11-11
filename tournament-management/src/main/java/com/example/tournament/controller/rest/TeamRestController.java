package com.example.tournament.controller.rest;

import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.dto.request.TeamFilter;
import com.example.tournament.dto.request.UpdateTeamRequest;
import com.example.tournament.dto.response.PageResponse;
import com.example.tournament.dto.response.TeamHistoryResponse;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.service.TeamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/teams")
@RequiredArgsConstructor
public class TeamRestController {

    private final TeamService teamService;

    @PostMapping
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request) {
        TeamResponse created = teamService.create(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}")
                .buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    /** Exemple : GET /api/teams?page=0&size=10&sort=name,asc&search=lions&city=Tana&status=ACTIVE */
    @GetMapping
    public PageResponse<TeamResponse> list(TeamFilter filter, Pageable pageable) {
        return PageResponse.from(teamService.search(filter, pageable));
    }

    @GetMapping("/{id}")
    public TeamResponse get(@PathVariable Long id) {
        return teamService.get(id);
    }

    @PutMapping("/{id}")
    public TeamResponse update(@PathVariable Long id, @Valid @RequestBody UpdateTeamRequest request) {
        return teamService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teamService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** Participations aux tournois et matchs de l'équipe. */
    @GetMapping("/{id}/history")
    public TeamHistoryResponse history(@PathVariable Long id) {
        return teamService.history(id);
    }
}

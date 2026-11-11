package com.example.tournament.controller.rest;

import com.example.tournament.dto.request.MatchResultRequest;
import com.example.tournament.dto.request.RescheduleMatchRequest;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
@RequiredArgsConstructor
public class MatchRestController {

    private final MatchService matchService;

    @GetMapping("/{id}")
    public MatchResponse get(@PathVariable Long id) {
        return matchService.get(id);
    }

    /**
     * Enregistre ou corrige un résultat. Le vainqueur est déterminé automatiquement, l'équipe
     * qualifiée dans le match suivant (élimination directe) et le classement recalculé.
     */
    @PutMapping("/{id}/result")
    public MatchResponse recordResult(@PathVariable Long id, @Valid @RequestBody MatchResultRequest request) {
        return matchService.recordResult(id, request);
    }

    @PutMapping("/{id}/schedule")
    public MatchResponse reschedule(@PathVariable Long id, @Valid @RequestBody RescheduleMatchRequest request) {
        return matchService.reschedule(id, request);
    }
}

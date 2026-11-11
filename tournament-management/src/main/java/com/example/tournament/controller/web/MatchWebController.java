package com.example.tournament.controller.web;

import com.example.tournament.dto.request.MatchResultRequest;
import com.example.tournament.dto.request.RescheduleMatchRequest;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.service.MatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Saisie des résultats et reprogrammation des matchs. */
@Controller
@RequestMapping("/matches")
@RequiredArgsConstructor
public class MatchWebController {

    private final MatchService matchService;

    @GetMapping("/{id}/result")
    public String resultForm(@PathVariable Long id, Model model) {
        MatchResponse match = matchService.get(id);
        MatchResultRequest form = new MatchResultRequest();
        form.setHomeScore(match.homeScore());
        form.setAwayScore(match.awayScore());
        form.setHomePenalties(match.homePenalties());
        form.setAwayPenalties(match.awayPenalties());
        model.addAttribute("match", match);
        model.addAttribute("form", form);
        return "matches/result";
    }

    @PostMapping("/{id}/result")
    public String result(@PathVariable Long id, @Valid @ModelAttribute("form") MatchResultRequest form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                MatchResponse saved = matchService.recordResult(id, form);
                redirect.addFlashAttribute("success", "Résultat enregistré : le classement et le tableau "
                        + "ont été mis à jour");
                return "redirect:/tournaments/" + saved.tournamentId() + "/matches";
            } catch (BusinessException ex) {
                binding.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("match", matchService.get(id));
        return "matches/result";
    }

    @GetMapping("/{id}/reschedule")
    public String rescheduleForm(@PathVariable Long id, Model model) {
        MatchResponse match = matchService.get(id);
        RescheduleMatchRequest form = new RescheduleMatchRequest();
        form.setScheduledAt(match.scheduledAt());
        form.setVenue(match.venue());
        model.addAttribute("match", match);
        model.addAttribute("form", form);
        return "matches/reschedule";
    }

    @PostMapping("/{id}/reschedule")
    public String reschedule(@PathVariable Long id, @Valid @ModelAttribute("form") RescheduleMatchRequest form,
                             BindingResult binding, Model model, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                MatchResponse saved = matchService.reschedule(id, form);
                redirect.addFlashAttribute("success", "Match reprogrammé");
                return "redirect:/tournaments/" + saved.tournamentId() + "/matches";
            } catch (BusinessException ex) {
                binding.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("match", matchService.get(id));
        return "matches/reschedule";
    }
}

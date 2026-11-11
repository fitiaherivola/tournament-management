package com.example.tournament.controller.web;

import com.example.tournament.dto.request.CreateTournamentRequest;
import com.example.tournament.dto.request.GenerateMatchesRequest;
import com.example.tournament.dto.request.MatchFilter;
import com.example.tournament.dto.request.RankingConfigRequest;
import com.example.tournament.dto.request.TournamentFilter;
import com.example.tournament.dto.request.UpdateTournamentRequest;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.entity.MatchPhase;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.entity.RankingCriterion;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.entity.TournamentType;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.service.MatchGenerationService;
import com.example.tournament.service.MatchService;
import com.example.tournament.service.RankingService;
import com.example.tournament.service.RegistrationService;
import com.example.tournament.service.StatisticsService;
import com.example.tournament.service.TournamentService;
import com.example.tournament.util.QueryStrings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Interface Thymeleaf des tournois : @Controller vers @Service vers @Repository.
 * Aucune règle métier ici : les refus (tournoi terminé, doublon...) viennent des services et sont
 * affichés par {@code GlobalWebExceptionHandler} ou directement dans le formulaire.
 */
@Controller
@RequestMapping("/tournaments")
@RequiredArgsConstructor
public class TournamentWebController {

    private static final int RANKING_SLOTS = 4;

    private final TournamentService tournamentService;
    private final RegistrationService registrationService;
    private final MatchGenerationService generationService;
    private final MatchService matchService;
    private final RankingService rankingService;
    private final StatisticsService statisticsService;
    private final Validator validator;

    @ModelAttribute("statuses")
    public TournamentStatus[] statuses() {
        return TournamentStatus.values();
    }

    @ModelAttribute("types")
    public TournamentType[] types() {
        return TournamentType.values();
    }

    // ------------------------------------------------------------------ liste et détail

    @GetMapping
    public String list(@ModelAttribute("filter") TournamentFilter filter, Pageable pageable,
                       HttpServletRequest request, Model model) {
        model.addAttribute("page", tournamentService.search(filter, pageable));
        model.addAttribute("path", "/tournaments");
        model.addAttribute("qs", QueryStrings.without(request, "page"));
        return "tournaments/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        TournamentResponse tournament = tournamentService.get(id);
        model.addAttribute("tournament", tournament);
        model.addAttribute("registrations", registrationService.list(id, false));
        if (tournament.status() == TournamentStatus.INSCRIPTION) {
            model.addAttribute("availableTeams", registrationService.availableTeams(id));
        }
        return "tournaments/detail";
    }

    // ------------------------------------------------------------------ création et modification

    @GetMapping("/new")
    public String newForm(Model model) {
        return form(model, new CreateTournamentRequest(), "/tournaments", "Nouveau tournoi");
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CreateTournamentRequest form, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                TournamentResponse created = tournamentService.create(form);
                redirect.addFlashAttribute("success", "Tournoi « " + created.name() + " » créé");
                return "redirect:/tournaments/" + created.id();
            } catch (BusinessException ex) {
                binding.reject("business", ex.getMessage());
            }
        }
        return form(model, null, "/tournaments", "Nouveau tournoi");
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TournamentResponse t = tournamentService.get(id);
        UpdateTournamentRequest form = new UpdateTournamentRequest();
        form.setName(t.name());
        form.setSport(t.sport());
        form.setDescription(t.description());
        form.setStartDate(t.startDate());
        form.setEndDate(t.endDate());
        form.setLocation(t.location());
        form.setMaxTeams(t.maxTeams());
        form.setType(t.type());
        return form(model, form, "/tournaments/" + id + "/edit", "Modifier « " + t.name() + " »");
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") UpdateTournamentRequest form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                tournamentService.update(id, form);
                redirect.addFlashAttribute("success", "Tournoi mis à jour");
                return "redirect:/tournaments/" + id;
            } catch (BusinessException ex) {
                binding.reject("business", ex.getMessage());
            }
        }
        return form(model, null, "/tournaments/" + id + "/edit", "Modifier le tournoi");
    }

    // ------------------------------------------------------------------ cycle de vie

    @PostMapping("/{id}/open-registration")
    public String openRegistration(@PathVariable Long id, RedirectAttributes redirect) {
        tournamentService.openRegistration(id);
        redirect.addFlashAttribute("success", "Les inscriptions sont ouvertes");
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable Long id, RedirectAttributes redirect) {
        tournamentService.start(id);
        redirect.addFlashAttribute("success", "Le tournoi a démarré : les résultats peuvent être saisis");
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, RedirectAttributes redirect) {
        tournamentService.cancel(id);
        redirect.addFlashAttribute("success", "Le tournoi a été annulé");
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        tournamentService.delete(id);
        redirect.addFlashAttribute("success", "Tournoi supprimé");
        return "redirect:/tournaments";
    }

    // ------------------------------------------------------------------ équipes inscrites

    @PostMapping("/{id}/teams")
    public String register(@PathVariable Long id, @RequestParam Long teamId, RedirectAttributes redirect) {
        var registration = registrationService.register(id, teamId);
        redirect.addFlashAttribute("success", "Équipe « " + registration.teamName() + " » inscrite");
        return "redirect:/tournaments/" + id;
    }

    @PostMapping("/{id}/teams/{teamId}/withdraw")
    public String withdraw(@PathVariable Long id, @PathVariable Long teamId, RedirectAttributes redirect) {
        registrationService.withdraw(id, teamId);
        redirect.addFlashAttribute("success", "Équipe retirée du tournoi");
        return "redirect:/tournaments/" + id;
    }

    // ------------------------------------------------------------------ calendrier

    @GetMapping("/{id}/generate")
    public String generateForm(@PathVariable Long id, Model model) {
        TournamentResponse tournament = tournamentService.get(id);
        GenerateMatchesRequest form = new GenerateMatchesRequest();
        form.setStartDateTime(tournament.startDate().atTime(10, 0));
        form.setSlotMinutes(120);
        form.setRoundIntervalDays(1);
        model.addAttribute("tournament", tournament);
        model.addAttribute("form", form);
        return "tournaments/generate";
    }

    @PostMapping("/{id}/generate")
    public String generate(@PathVariable Long id, @Valid @ModelAttribute("form") GenerateMatchesRequest form,
                           BindingResult binding, Model model, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                int count = generationService.generate(id, form).size();
                redirect.addFlashAttribute("success", count + " matchs générés");
                return "redirect:/tournaments/" + id + "/matches";
            } catch (BusinessException ex) {
                binding.reject("business", ex.getMessage());
            }
        }
        model.addAttribute("tournament", tournamentService.get(id));
        return "tournaments/generate";
    }

    @PostMapping("/{id}/reset-matches")
    public String resetMatches(@PathVariable Long id, RedirectAttributes redirect) {
        generationService.reset(id);
        redirect.addFlashAttribute("success", "Calendrier supprimé : il peut être régénéré");
        return "redirect:/tournaments/" + id;
    }

    @GetMapping("/{id}/matches")
    public String matches(@PathVariable Long id, @ModelAttribute("filter") MatchFilter filter, Pageable pageable,
                          HttpServletRequest request, Model model) {
        model.addAttribute("tournament", tournamentService.get(id));
        model.addAttribute("page", matchService.search(id, filter, pageable));
        model.addAttribute("teams", registrationService.list(id, false));
        model.addAttribute("phases", MatchPhase.values());
        model.addAttribute("matchStatuses", MatchStatus.values());
        model.addAttribute("path", "/tournaments/" + id + "/matches");
        model.addAttribute("qs", QueryStrings.without(request, "page"));
        return "tournaments/matches";
    }

    // ------------------------------------------------------------------ classement, arbre, statistiques

    @GetMapping("/{id}/ranking")
    public String ranking(@PathVariable Long id, Model model) {
        TournamentResponse tournament = tournamentService.get(id);
        if (!tournament.type().isChampionship()) {
            return "redirect:/tournaments/" + id + "/bracket";
        }
        model.addAttribute("tournament", tournament);
        model.addAttribute("standings", rankingService.ranking(id));
        return "tournaments/ranking";
    }

    @GetMapping("/{id}/bracket")
    public String bracket(@PathVariable Long id, Model model) {
        TournamentResponse tournament = tournamentService.get(id);
        if (tournament.type().isChampionship()) {
            return "redirect:/tournaments/" + id + "/ranking";
        }
        model.addAttribute("tournament", tournament);
        model.addAttribute("bracket", matchService.bracket(id));
        return "tournaments/bracket";
    }

    @GetMapping("/{id}/statistics")
    public String statistics(@PathVariable Long id, Model model) {
        model.addAttribute("tournament", tournamentService.get(id));
        model.addAttribute("stats", statisticsService.tournamentStatistics(id));
        return "tournaments/statistics";
    }

    // ------------------------------------------------------------------ configuration du classement (ADMIN)

    @GetMapping("/{id}/ranking-config")
    public String rankingConfigForm(@PathVariable Long id, Model model) {
        TournamentResponse tournament = tournamentService.get(id);
        List<RankingCriterion> slots = new ArrayList<>(tournament.rankingCriteria());
        while (slots.size() < RANKING_SLOTS) {
            slots.add(null);
        }
        model.addAttribute("tournament", tournament);
        model.addAttribute("slots", slots);
        model.addAttribute("criteriaValues", RankingCriterion.values());
        return "tournaments/ranking-config";
    }

    @PostMapping("/{id}/ranking-config")
    public String rankingConfig(@PathVariable Long id,
                                @RequestParam(required = false) List<String> criteria,
                                @RequestParam Integer pointsForWin,
                                @RequestParam Integer pointsForDraw,
                                @RequestParam Integer pointsForLoss,
                                RedirectAttributes redirect) {
        RankingConfigRequest request = new RankingConfigRequest();
        List<RankingCriterion> parsed = new ArrayList<>();
        if (criteria != null) {
            for (String value : criteria) {
                if (value != null && !value.isBlank()) {
                    try {
                        parsed.add(RankingCriterion.valueOf(value));
                    } catch (IllegalArgumentException e) {
                        throw new BusinessException("Critère de classement inconnu : " + value);
                    }
                }
            }
        }
        request.setCriteria(parsed);
        request.setPointsForWin(pointsForWin);
        request.setPointsForDraw(pointsForDraw);
        request.setPointsForLoss(pointsForLoss);

        var violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new BusinessException(violations.stream().map(ConstraintViolation::getMessage)
                    .sorted().collect(Collectors.joining(" ; ")));
        }
        tournamentService.updateRankingConfig(id, request);
        redirect.addFlashAttribute("success", "Configuration du classement enregistrée");
        return "redirect:/tournaments/" + id + "/ranking";
    }

    // ------------------------------------------------------------------ utilitaire

    private String form(Model model, Object form, String action, String title) {
        if (form != null) {
            model.addAttribute("form", form);
        }
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        return "tournaments/form";
    }
}

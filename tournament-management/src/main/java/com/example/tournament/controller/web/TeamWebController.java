package com.example.tournament.controller.web;

import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.dto.request.TeamFilter;
import com.example.tournament.dto.request.UpdateTeamRequest;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.entity.TeamStatus;
import com.example.tournament.exception.BusinessException;
import com.example.tournament.service.TeamService;
import com.example.tournament.util.QueryStrings;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Gestion des équipes dans l'interface Thymeleaf (mêmes services que l'API REST). */
@Controller
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamWebController {

    private final TeamService teamService;

    @ModelAttribute("teamStatuses")
    public TeamStatus[] teamStatuses() {
        return TeamStatus.values();
    }

    @GetMapping
    public String list(@ModelAttribute("filter") TeamFilter filter, Pageable pageable,
                       HttpServletRequest request, Model model) {
        model.addAttribute("page", teamService.search(filter, pageable));
        model.addAttribute("path", "/teams");
        model.addAttribute("qs", QueryStrings.without(request, "page"));
        return "teams/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("history", teamService.history(id));
        return "teams/detail";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        CreateTeamRequest form = new CreateTeamRequest();
        form.setStatus(TeamStatus.ACTIVE);
        return form(model, form, "/teams", "Nouvelle équipe");
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") CreateTeamRequest form, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                TeamResponse created = teamService.create(form);
                redirect.addFlashAttribute("success", "Équipe « " + created.name() + " » créée");
                return "redirect:/teams/" + created.id();
            } catch (BusinessException ex) {
                binding.reject("business", ex.getMessage());
            }
        }
        return form(model, null, "/teams", "Nouvelle équipe");
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        TeamResponse team = teamService.get(id);
        UpdateTeamRequest form = new UpdateTeamRequest();
        form.setName(team.name());
        form.setLogo(team.logo());
        form.setCity(team.city());
        form.setCoach(team.coach());
        form.setPlayers(new java.util.ArrayList<>(team.players()));
        form.setStatus(team.status());
        return form(model, form, "/teams/" + id + "/edit", "Modifier l'équipe « " + team.name() + " »");
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("form") UpdateTeamRequest form,
                         BindingResult binding, Model model, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            try {
                TeamResponse updated = teamService.update(id, form);
                redirect.addFlashAttribute("success", "Équipe « " + updated.name() + " » mise à jour");
                return "redirect:/teams/" + id;
            } catch (BusinessException ex) {
                binding.reject("business", ex.getMessage());
            }
        }
        return form(model, null, "/teams/" + id + "/edit", "Modifier l'équipe");
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        teamService.delete(id);
        redirect.addFlashAttribute("success", "Équipe supprimée");
        return "redirect:/teams";
    }

    private String form(Model model, Object form, String action, String title) {
        if (form != null) {
            model.addAttribute("form", form);
        }
        model.addAttribute("formAction", action);
        model.addAttribute("formTitle", title);
        return "teams/form";
    }
}

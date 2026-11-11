package com.example.tournament.controller.web;

import com.example.tournament.service.StatisticsService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Tableau de bord, page de connexion et page « accès refusé ». */
@Controller
@RequiredArgsConstructor
public class HomeWebController {

    private final StatisticsService statisticsService;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("dashboard", statisticsService.dashboard());
        return "dashboard";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/error/403")
    public String forbidden(Model model, HttpServletResponse response) {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        model.addAttribute("status", 403);
        model.addAttribute("title", "Accès refusé");
        model.addAttribute("message", "Votre rôle ne vous autorise pas à effectuer cette opération.");
        return "error";
    }
}

package com.example.tournament.controller.rest;

import com.example.tournament.dto.response.DashboardResponse;
import com.example.tournament.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardRestController {

    private final StatisticsService statisticsService;

    @GetMapping
    public DashboardResponse dashboard() {
        return statisticsService.dashboard();
    }
}

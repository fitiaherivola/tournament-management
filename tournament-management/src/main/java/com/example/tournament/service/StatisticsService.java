package com.example.tournament.service;

import com.example.tournament.dto.response.DashboardResponse;
import com.example.tournament.dto.response.TournamentStatisticsResponse;

public interface StatisticsService {

    TournamentStatisticsResponse tournamentStatistics(Long tournamentId);

    DashboardResponse dashboard();
}

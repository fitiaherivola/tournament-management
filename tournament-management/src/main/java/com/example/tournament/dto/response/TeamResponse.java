package com.example.tournament.dto.response;

import com.example.tournament.entity.TeamStatus;

import java.util.List;

public record TeamResponse(
        Long id,
        String name,
        String logo,
        String city,
        String coach,
        List<String> players,
        TeamStatus status) {
}

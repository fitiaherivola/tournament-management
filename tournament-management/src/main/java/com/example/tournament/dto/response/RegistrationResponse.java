package com.example.tournament.dto.response;

import com.example.tournament.entity.RegistrationStatus;

import java.time.LocalDateTime;

public record RegistrationResponse(
        Long id,
        Long tournamentId,
        Long teamId,
        String teamName,
        String city,
        LocalDateTime registeredAt,
        LocalDateTime withdrawnAt,
        RegistrationStatus status) {
}

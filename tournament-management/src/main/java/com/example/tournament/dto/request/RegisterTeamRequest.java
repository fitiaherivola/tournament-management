package com.example.tournament.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegisterTeamRequest {

    @NotNull(message = "L'identifiant de l'équipe est obligatoire")
    @Positive(message = "L'identifiant de l'équipe doit être positif")
    private Long teamId;
}

package com.example.tournament.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MatchResultRequest {

    @NotNull(message = "Le score de l'équipe à domicile est obligatoire")
    @PositiveOrZero(message = "Le score ne peut pas être négatif")
    @Max(value = 99, message = "Score invalide")
    private Integer homeScore;

    @NotNull(message = "Le score de l'équipe visiteuse est obligatoire")
    @PositiveOrZero(message = "Le score ne peut pas être négatif")
    @Max(value = 99, message = "Score invalide")
    private Integer awayScore;

    /** Tirs au but : obligatoires uniquement pour un match nul en élimination directe. */
    @PositiveOrZero(message = "Les tirs au but ne peuvent pas être négatifs")
    @Max(value = 99, message = "Valeur invalide")
    private Integer homePenalties;

    @PositiveOrZero(message = "Les tirs au but ne peuvent pas être négatifs")
    @Max(value = 99, message = "Valeur invalide")
    private Integer awayPenalties;
}

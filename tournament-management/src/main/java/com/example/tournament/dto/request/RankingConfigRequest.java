package com.example.tournament.dto.request;

import com.example.tournament.entity.RankingCriterion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** Configuration du classement (réservée à l'administrateur). */
@Getter
@Setter
@NoArgsConstructor
public class RankingConfigRequest {

    /** Critères de départage, dans l'ordre d'application. */
    @NotEmpty(message = "Au moins un critère de classement est obligatoire")
    private List<@NotNull(message = "Critère invalide") RankingCriterion> criteria = new ArrayList<>();

    @NotNull(message = "Les points de victoire sont obligatoires")
    @Min(value = 0, message = "Les points ne peuvent pas être négatifs")
    @Max(value = 10, message = "Maximum 10 points")
    private Integer pointsForWin = 3;

    @NotNull(message = "Les points de match nul sont obligatoires")
    @Min(value = 0, message = "Les points ne peuvent pas être négatifs")
    @Max(value = 10, message = "Maximum 10 points")
    private Integer pointsForDraw = 1;

    @NotNull(message = "Les points de défaite sont obligatoires")
    @Min(value = 0, message = "Les points ne peuvent pas être négatifs")
    @Max(value = 10, message = "Maximum 10 points")
    private Integer pointsForLoss = 0;
}

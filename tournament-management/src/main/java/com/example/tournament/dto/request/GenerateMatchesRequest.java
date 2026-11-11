package com.example.tournament.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Paramètres facultatifs de génération du calendrier. Toutes les valeurs ont un défaut
 * (début = date de début du tournoi à 10h, créneaux de 2h, un seul terrain).
 */
@Getter
@Setter
@NoArgsConstructor
public class GenerateMatchesRequest {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDateTime;

    @Min(value = 30, message = "Un créneau doit durer au moins 30 minutes")
    @Max(value = 600, message = "Un créneau ne peut pas dépasser 600 minutes")
    private Integer slotMinutes;

    @Min(value = 1, message = "L'intervalle entre deux tours est d'au moins 1 jour")
    @Max(value = 60, message = "L'intervalle entre deux tours ne peut pas dépasser 60 jours")
    private Integer roundIntervalDays;

    /** Terrains disponibles (les matchs d'un même tour sont répartis dessus). */
    @Size(max = 20, message = "20 terrains au maximum")
    private List<@NotBlank(message = "Le nom d'un terrain ne peut pas être vide") String> venues = new ArrayList<>();

    /** Tirage au sort de l'ordre des équipes (sinon : ordre d'inscription). */
    private boolean shuffle;
}

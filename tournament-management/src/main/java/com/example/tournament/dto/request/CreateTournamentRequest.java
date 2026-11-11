package com.example.tournament.dto.request;

import com.example.tournament.entity.TournamentType;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class CreateTournamentRequest {

    @NotBlank(message = "Le nom du tournoi est obligatoire")
    @Size(max = 120, message = "Le nom ne doit pas dépasser 120 caractères")
    private String name;

    @NotBlank(message = "Le sport est obligatoire")
    @Size(max = 60, message = "Le sport ne doit pas dépasser 60 caractères")
    private String sport;

    @Size(max = 2000, message = "La description ne doit pas dépasser 2000 caractères")
    private String description;

    @NotNull(message = "La date de début est obligatoire")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @NotNull(message = "La date de fin est obligatoire")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    @NotBlank(message = "Le lieu est obligatoire")
    @Size(max = 150, message = "Le lieu ne doit pas dépasser 150 caractères")
    private String location;

    @NotNull(message = "Le nombre maximum d'équipes est obligatoire")
    @Min(value = 2, message = "Un tournoi doit accepter au moins 2 équipes")
    @Max(value = 64, message = "Un tournoi ne peut pas dépasser 64 équipes")
    private Integer maxTeams;

    @NotNull(message = "Le type de tournoi est obligatoire")
    private TournamentType type;

    @JsonIgnore
    @AssertTrue(message = "La date de fin doit être postérieure ou égale à la date de début")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}

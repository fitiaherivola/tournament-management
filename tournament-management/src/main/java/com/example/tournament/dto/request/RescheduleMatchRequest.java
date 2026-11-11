package com.example.tournament.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class RescheduleMatchRequest {

    @NotNull(message = "La date et l'heure du match sont obligatoires")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime scheduledAt;

    @NotBlank(message = "Le terrain est obligatoire")
    @Size(max = 100, message = "Le nom du terrain ne doit pas dépasser 100 caractères")
    private String venue;
}

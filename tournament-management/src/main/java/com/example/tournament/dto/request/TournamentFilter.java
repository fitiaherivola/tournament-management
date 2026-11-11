package com.example.tournament.dto.request;

import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.entity.TournamentType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Critères de recherche des tournois (paramètres de requête). */
@Getter
@Setter
@NoArgsConstructor
public class TournamentFilter {

    /** Recherche texte sur le nom, le sport et le lieu. */
    private String search;
    private TournamentStatus status;
    private TournamentType type;
    private String sport;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startTo;
}

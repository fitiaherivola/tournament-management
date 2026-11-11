package com.example.tournament.dto.request;

import com.example.tournament.entity.MatchPhase;
import com.example.tournament.entity.MatchStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/** Critères de filtrage des matchs d'un tournoi. */
@Getter
@Setter
@NoArgsConstructor
public class MatchFilter {

    private MatchPhase phase;
    private MatchStatus status;
    private Long teamId;
    private Integer round;
    private String venue;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate from;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate to;
}

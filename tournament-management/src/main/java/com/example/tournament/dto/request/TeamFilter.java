package com.example.tournament.dto.request;

import com.example.tournament.entity.TeamStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Critères de recherche des équipes (nom, ville, entraîneur + statut). */
@Getter
@Setter
@NoArgsConstructor
public class TeamFilter {

    private String search;
    private String city;
    private TeamStatus status;
}

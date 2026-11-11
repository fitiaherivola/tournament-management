package com.example.tournament.dto.request;

import com.example.tournament.entity.TeamStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateTeamRequest {

    @NotBlank(message = "Le nom de l'équipe est obligatoire")
    @Size(max = 100, message = "Le nom ne doit pas dépasser 100 caractères")
    private String name;

    @Size(max = 500, message = "Le logo (URL) ne doit pas dépasser 500 caractères")
    private String logo;

    @NotBlank(message = "La ville est obligatoire")
    @Size(max = 100, message = "La ville ne doit pas dépasser 100 caractères")
    private String city;

    @NotBlank(message = "L'entraîneur est obligatoire")
    @Size(max = 100, message = "Le nom de l'entraîneur ne doit pas dépasser 100 caractères")
    private String coach;

    @Size(max = 50, message = "Une équipe ne peut pas compter plus de 50 joueurs")
    private List<@NotBlank(message = "Le nom d'un joueur ne peut pas être vide")
            @Size(max = 100, message = "Le nom d'un joueur ne doit pas dépasser 100 caractères") String> players = new ArrayList<>();

    /** Facultatif : ACTIVE par défaut. */
    private TeamStatus status;
}

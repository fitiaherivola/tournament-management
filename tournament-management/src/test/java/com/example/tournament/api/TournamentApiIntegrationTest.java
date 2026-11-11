package com.example.tournament.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours complets de l'API REST à travers toutes les couches (sécurité, validation, services,
 * transactions, base H2) : codes HTTP, rôles, règles métier et recalcul du classement.
 */
@SpringBootTest
@AutoConfigureMockMvc
class TournamentApiIntegrationTest {

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");
    private static final RequestPostProcessor MANAGER = user("manager").roles("MANAGER");
    private static final RequestPostProcessor OPERATOR = user("operator").roles("OPERATOR");
    private static final RequestPostProcessor USER = user("user").roles("USER");

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    // ------------------------------------------------------------------ sécurité

    @Test
    void anonymousCallGets401WithJsonBody() throws Exception {
        mvc.perform(get("/api/tournaments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void userCanReadButNotWrite() throws Exception {
        mvc.perform(get("/api/tournaments").with(USER)).andExpect(status().isOk());
        mvc.perform(post("/api/tournaments").with(USER).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(tournamentBody("CHAMPIONNAT_SIMPLE", 8))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("ACCESS_DENIED"));
    }

    @Test
    void rankingConfigurationAndDeletionAreReservedToAdmin() throws Exception {
        long id = createTournament("CHAMPIONNAT_SIMPLE", 8);
        String config = "{\"criteria\":[\"POINTS\",\"GOALS_FOR\"],\"pointsForWin\":2,\"pointsForDraw\":1,\"pointsForLoss\":0}";

        mvc.perform(put("/api/tournaments/{id}/ranking-config", id).with(MANAGER)
                .contentType(MediaType.APPLICATION_JSON).content(config)).andExpect(status().isForbidden());
        mvc.perform(put("/api/tournaments/{id}/ranking-config", id).with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON).content(config))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsForWin").value(2))
                .andExpect(jsonPath("$.rankingCriteria.length()").value(2));
        // barème incohérent : victoire < nul
        mvc.perform(put("/api/tournaments/{id}/ranking-config", id).with(ADMIN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"criteria\":[\"POINTS\"],\"pointsForWin\":0,\"pointsForDraw\":1,\"pointsForLoss\":0}"))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(delete("/api/tournaments/{id}", id).with(MANAGER)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/tournaments/{id}", id).with(ADMIN)).andExpect(status().isNoContent());
        mvc.perform(get("/api/tournaments/{id}", id).with(USER)).andExpect(status().isNotFound());
    }

    // ------------------------------------------------------------------ validation et erreurs

    @Test
    void createReturns201WithLocationAndDto() throws Exception {
        mvc.perform(post("/api/tournaments").with(MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(tournamentBody("ELIMINATION_DIRECTE", 16))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("BROUILLON"))
                .andExpect(jsonPath("$.type").value("ELIMINATION_DIRECTE"));
    }

    @Test
    void invalidBodyReturns400WithFieldViolations() throws Exception {
        Map<String, Object> body = tournamentBody("CHAMPIONNAT_SIMPLE", 1);
        body.put("name", " ");
        body.put("endDate", LocalDate.now().minusDays(5).toString());

        mvc.perform(post("/api/tournaments").with(MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.violations[?(@.field=='name')]").exists())
                .andExpect(jsonPath("$.violations[?(@.field=='maxTeams')]").exists());
    }

    @Test
    void malformedJsonAndUnknownEnumReturn400() throws Exception {
        mvc.perform(post("/api/tournaments").with(MANAGER).contentType(MediaType.APPLICATION_JSON).content("{oops"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
        mvc.perform(post("/api/tournaments").with(MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(tournamentBody("INCONNU", 8))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownResourcesReturn404() throws Exception {
        mvc.perform(get("/api/tournaments/{id}", 987654).with(USER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        mvc.perform(get("/api/teams/{id}", 987654).with(USER)).andExpect(status().isNotFound());
        mvc.perform(get("/api/matches/{id}", 987654).with(USER)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateTeamNameReturns409() throws Exception {
        String name = "Doublon " + UUID.randomUUID();
        Map<String, Object> team = Map.of("name", name, "city", "Tana", "coach", "Coach");
        mvc.perform(post("/api/teams").with(MANAGER).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(team))).andExpect(status().isCreated());
        mvc.perform(post("/api/teams").with(MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(team)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    // ------------------------------------------------------------------ recherche, tri, pagination

    @Test
    void listsArePaginatedSortedAndFiltered() throws Exception {
        String sport = "Sport" + UUID.randomUUID().toString().substring(0, 8);
        for (int i = 0; i < 3; i++) {
            Map<String, Object> body = tournamentBody("CHAMPIONNAT_SIMPLE", 8);
            body.put("sport", sport);
            body.put("name", "Tournoi " + (char) ('A' + i) + " " + sport);
            mvc.perform(post("/api/tournaments").with(MANAGER).contentType(MediaType.APPLICATION_JSON)
                    .content(json.writeValueAsString(body))).andExpect(status().isCreated());
        }

        mvc.perform(get("/api/tournaments").with(USER)
                        .param("sport", sport).param("size", "2").param("page", "0").param("sort", "name,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.content[0].name").value("Tournoi C " + sport));
        mvc.perform(get("/api/tournaments").with(USER).param("sport", sport).param("status", "EN_COURS"))
                .andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(get("/api/tournaments").with(USER).param("search", sport.toLowerCase()))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/tournaments").with(USER).param("sort", "motDePasse,asc"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/tournaments").with(USER).param("status", "N_IMPORTE_QUOI"))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------ championnat

    @Test
    void championshipLifecycleGeneratesMatchesRanksTeamsAndLocksFinishedTournament() throws Exception {
        long tournament = createTournament("CHAMPIONNAT_SIMPLE", 6);
        List<Long> teams = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            teams.add(createTeam("Club"));
        }

        // inscriptions fermées tant que le tournoi est en brouillon
        registerTeam(tournament, teams.get(0)).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/tournaments/{id}/open-registration", tournament).with(MANAGER))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("INSCRIPTION"));
        for (Long team : teams) {
            registerTeam(tournament, team).andExpect(status().isCreated());
        }
        registerTeam(tournament, teams.get(0)).andExpect(status().isConflict());   // même équipe deux fois

        // génération : 4 équipes -> 6 matchs, une seule fois
        mvc.perform(post("/api/tournaments/{id}/generate-matches", tournament).with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(generationBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.length()").value(6));
        mvc.perform(post("/api/tournaments/{id}/generate-matches", tournament).with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(generationBody()))
                .andExpect(status().isConflict());
        registerTeam(tournament, createTeam("Retard")).andExpect(status().isUnprocessableEntity());

        // pas de résultat avant le démarrage ; un opérateur peut démarrer
        long first = idsOfMatches(tournament, "PROGRAMME").get(0);
        putResult(first, OPERATOR, 1, 0).andExpect(status().isUnprocessableEntity());
        mvc.perform(post("/api/tournaments/{id}/start", tournament).with(USER)).andExpect(status().isForbidden());
        mvc.perform(post("/api/tournaments/{id}/start", tournament).with(OPERATOR))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EN_COURS"));

        // saisie des résultats : droits, validation, vainqueur, classement
        putResult(first, USER, 3, 1).andExpect(status().isForbidden());
        putResult(first, OPERATOR, -1, 1).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
        putResult(first, OPERATOR, 3, 1).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TERMINE"))
                .andExpect(jsonPath("$.winner.id").isNumber());
        long homeId = ((Number) JsonPath.read(getBody("/api/matches/" + first), "$.homeTeam.id")).longValue();
        mvc.perform(get("/api/tournaments/{id}/ranking", tournament).with(USER))
                .andExpect(jsonPath("$[0].teamId").value(homeId))
                .andExpect(jsonPath("$[0].points").value(3))
                .andExpect(jsonPath("$[0].goalDifference").value(2));

        // corriger le résultat en match nul : le classement est recalculé
        putResult(first, OPERATOR, 1, 1).andExpect(status().isOk()).andExpect(jsonPath("$.winner").doesNotExist());
        mvc.perform(get("/api/tournaments/{id}/ranking", tournament).with(USER))
                .andExpect(jsonPath("$[0].points").value(1))
                .andExpect(jsonPath("$[1].points").value(1))
                .andExpect(jsonPath("$[2].points").value(0));

        // terminer tous les matchs : le tournoi se clôture automatiquement
        for (long match : idsOfMatches(tournament, "PROGRAMME")) {
            putResult(match, OPERATOR, 2, 0).andExpect(status().isOk());
        }
        mvc.perform(get("/api/tournaments/{id}", tournament).with(USER))
                .andExpect(jsonPath("$.status").value("TERMINE"));

        // tournoi terminé : plus aucune modification possible
        mvc.perform(put("/api/tournaments/{id}", tournament).with(MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(tournamentBody("CHAMPIONNAT_SIMPLE", 6))))
                .andExpect(status().isUnprocessableEntity());
        putResult(first, OPERATOR, 5, 0).andExpect(status().isUnprocessableEntity());
        mvc.perform(delete("/api/tournaments/{id}/teams/{teamId}", tournament, teams.get(0)).with(MANAGER))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(get("/api/tournaments/{id}/statistics", tournament).with(USER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMatches").value(6))
                .andExpect(jsonPath("$.playedMatches").value(6));
    }

    @Test
    void teamCanBeWithdrawnBeforeStartAndCalendarIsResetThenRegenerated() throws Exception {
        long tournament = createTournament("CHAMPIONNAT_ALLER_RETOUR", 6);
        List<Long> teams = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            teams.add(createTeam("Retrait"));
        }
        mvc.perform(post("/api/tournaments/{id}/open-registration", tournament).with(MANAGER)).andExpect(status().isOk());
        for (Long team : teams) {
            registerTeam(tournament, team).andExpect(status().isCreated());
        }
        mvc.perform(post("/api/tournaments/{id}/generate-matches", tournament).with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(generationBody()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(6)); // 3 équipes aller-retour

        mvc.perform(delete("/api/tournaments/{id}/teams/{teamId}", tournament, teams.get(2)).with(MANAGER))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/tournaments/{id}", tournament).with(USER))
                .andExpect(jsonPath("$.status").value("INSCRIPTION"))
                .andExpect(jsonPath("$.matchesGenerated").value(false))
                .andExpect(jsonPath("$.registeredTeams").value(2));
        mvc.perform(post("/api/tournaments/{id}/generate-matches", tournament).with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(generationBody()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void registrationCannotBeReopenedOnceTheCalendarIsGenerated() throws Exception {
        long tournament = createTournament("CHAMPIONNAT_SIMPLE", 4);
        mvc.perform(post("/api/tournaments/{id}/open-registration", tournament).with(MANAGER))
                .andExpect(status().isOk());
        registerTeam(tournament, createTeam("Verrou")).andExpect(status().isCreated());
        registerTeam(tournament, createTeam("Verrou")).andExpect(status().isCreated());
        mvc.perform(post("/api/tournaments/{id}/generate-matches", tournament).with(MANAGER)
                        .contentType(MediaType.APPLICATION_JSON).content(generationBody()))
                .andExpect(status().isCreated());

        // le calendrier existe : impossible de rouvrir les inscriptions (ce qui créerait une incohérence)
        mvc.perform(post("/api/tournaments/{id}/open-registration", tournament).with(MANAGER))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_OPERATION"));

        // supprimer le calendrier repasse en inscription : une nouvelle équipe peut alors être inscrite
        mvc.perform(delete("/api/tournaments/{id}/matches", tournament).with(MANAGER))
                .andExpect(status().isNoContent());
        registerTeam(tournament, createTeam("Verrou")).andExpect(status().isCreated());
    }

    // ------------------------------------------------------------------ élimination directe

    @Test
    void knockoutWithFiveTeamsUsesByesQualifiesWinnersAndCrownsAChampion() throws Exception {
        long tournament = createTournament("ELIMINATION_DIRECTE", 8);
        for (int i = 0; i < 5; i++) {
            long team = createTeam("Coupe");
            if (i == 0) {
                mvc.perform(post("/api/tournaments/{id}/open-registration", tournament).with(MANAGER))
                        .andExpect(status().isOk());
            }
            registerTeam(tournament, team).andExpect(status().isCreated());
        }
        mvc.perform(post("/api/tournaments/{id}/generate-matches", tournament).with(MANAGER)
                .contentType(MediaType.APPLICATION_JSON).content(generationBody())).andExpect(status().isCreated());

        // 5 équipes -> tableau de 8 : 3 tours, 3 exempts au premier tour
        String bracket = getBody("/api/tournaments/" + tournament + "/bracket");
        assertThat(JsonPath.<List<?>>read(bracket, "$.rounds")).hasSize(3);
        assertThat(JsonPath.<List<?>>read(bracket, "$.rounds[0].matches[?(@.status=='EXEMPT')]")).hasSize(3);
        mvc.perform(get("/api/tournaments/{id}/ranking", tournament).with(USER)).andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/api/tournaments/{id}/start", tournament).with(OPERATOR)).andExpect(status().isOk());

        boolean triedDraw = false;
        for (int guard = 0; guard < 10; guard++) {
            List<Integer> playable = idsOfMatches(tournament, "PROGRAMME").stream().map(Long::intValue).toList();
            if (playable.isEmpty()) {
                break;
            }
            long match = playable.get(0);
            if (!triedDraw) {
                // un nul sans tirs au but est refusé en élimination directe
                putResult(match, OPERATOR, 1, 1).andExpect(status().isUnprocessableEntity());
                triedDraw = true;
            }
            putResult(match, OPERATOR, 2, 1).andExpect(status().isOk());
        }

        mvc.perform(get("/api/tournaments/{id}", tournament).with(USER)).andExpect(jsonPath("$.status").value("TERMINE"));
        mvc.perform(get("/api/tournaments/{id}/bracket", tournament).with(USER))
                .andExpect(jsonPath("$.champion.id").isNumber());
    }

    @Test
    void knockoutDrawIsDecidedByPenalties() throws Exception {
        long tournament = createTournament("ELIMINATION_DIRECTE", 2);
        mvc.perform(post("/api/tournaments/{id}/open-registration", tournament).with(MANAGER)).andExpect(status().isOk());
        registerTeam(tournament, createTeam("Tab")).andExpect(status().isCreated());
        registerTeam(tournament, createTeam("Tab")).andExpect(status().isCreated());
        mvc.perform(post("/api/tournaments/{id}/generate-matches", tournament).with(MANAGER)
                .contentType(MediaType.APPLICATION_JSON).content(generationBody())).andExpect(status().isCreated());
        mvc.perform(post("/api/tournaments/{id}/start", tournament).with(OPERATOR)).andExpect(status().isOk());
        long match = idsOfMatches(tournament, "PROGRAMME").get(0);

        mvc.perform(put("/api/matches/{id}/result", match).with(OPERATOR).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"homeScore\":2,\"awayScore\":2,\"homePenalties\":4,\"awayPenalties\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.winner.id").isNumber())
                .andExpect(jsonPath("$.awayPenalties").value(5));
        mvc.perform(get("/api/tournaments/{id}", tournament).with(USER)).andExpect(jsonPath("$.status").value("TERMINE"));
    }

    // ------------------------------------------------------------------ utilitaires

    private Map<String, Object> tournamentBody(String type, int maxTeams) {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("name", "Tournoi " + UUID.randomUUID());
        body.put("sport", "Football");
        body.put("description", "Tournoi de test");
        body.put("startDate", LocalDate.now().plusDays(1).toString());
        body.put("endDate", LocalDate.now().plusDays(10).toString());
        body.put("location", "Stade municipal");
        body.put("maxTeams", maxTeams);
        body.put("type", type);
        return body;
    }

    private long createTournament(String type, int maxTeams) throws Exception {
        MvcResult result = mvc.perform(post("/api/tournaments").with(MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(tournamentBody(type, maxTeams))))
                .andExpect(status().isCreated()).andReturn();
        return idOf(result);
    }

    private long createTeam(String prefix) throws Exception {
        Map<String, Object> team = Map.of("name", prefix + " " + UUID.randomUUID(), "city", "Antananarivo",
                "coach", "Coach", "players", List.of("Joueur 1", "Joueur 2"));
        MvcResult result = mvc.perform(post("/api/teams").with(MANAGER).contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(team)))
                .andExpect(status().isCreated()).andReturn();
        return idOf(result);
    }

    private org.springframework.test.web.servlet.ResultActions registerTeam(long tournament, long team) throws Exception {
        return mvc.perform(post("/api/tournaments/{id}/teams", tournament).with(MANAGER)
                .contentType(MediaType.APPLICATION_JSON).content("{\"teamId\":" + team + "}"));
    }

    private org.springframework.test.web.servlet.ResultActions putResult(long match, RequestPostProcessor who,
                                                                         int home, int away) throws Exception {
        return mvc.perform(put("/api/matches/{id}/result", match).with(who).contentType(MediaType.APPLICATION_JSON)
                .content("{\"homeScore\":" + home + ",\"awayScore\":" + away + "}"));
    }

    /** Les matchs d'un tournoi ayant un statut donné, du premier tour au dernier. */
    private List<Long> idsOfMatches(long tournament, String status) throws Exception {
        String body = mvc.perform(get("/api/tournaments/{id}/matches", tournament).with(USER)
                        .param("status", status).param("size", "100").param("sort", "round,asc"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        List<Integer> ids = JsonPath.read(body, "$.content[*].id");
        return ids.stream().map(Integer::longValue).toList();
    }

    private String getBody(String url) throws Exception {
        return mvc.perform(get(url).with(USER)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    /** Venue unique par génération : les terrains sont partagés entre tournois, donc entre tests. */
    private String generationBody() throws Exception {
        return json.writeValueAsString(Map.of("venues", List.of("Terrain-" + UUID.randomUUID())));
    }

    private static long idOf(MvcResult result) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.id")).longValue();
    }
}

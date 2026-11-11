package com.example.tournament.config;

import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.dto.request.CreateTournamentRequest;
import com.example.tournament.dto.request.GenerateMatchesRequest;
import com.example.tournament.dto.request.MatchFilter;
import com.example.tournament.dto.request.MatchResultRequest;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.entity.AppUser;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.entity.Role;
import com.example.tournament.entity.TournamentType;
import com.example.tournament.repository.AppUserRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.service.MatchGenerationService;
import com.example.tournament.service.MatchService;
import com.example.tournament.service.RegistrationService;
import com.example.tournament.service.TeamService;
import com.example.tournament.service.TournamentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Données de démonstration (désactivables avec {@code app.seed-demo-data=false}) :
 * quatre comptes, douze équipes et quatre tournois à des stades différents. Tout passe par la couche
 * service : les règles métier s'appliquent exactement comme pour un utilisateur réel.
 * <p>
 * Comptes : admin / admin123, manager / manager123, operator / operator123, user / user123.
 * À remplacer impérativement hors démonstration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private static final String[][] TEAMS = {
            {"Lémuriens de Tana", "Antananarivo"}, {"Zébus de Mahajanga", "Mahajanga"},
            {"Baobabs de Toliara", "Toliara"}, {"Requins de Toamasina", "Toamasina"},
            {"Vanilles de Sambava", "Sambava"}, {"Pirates de Nosy Be", "Nosy Be"},
            {"Caméléons d'Antsirabe", "Antsirabe"}, {"Fosas de Fianarantsoa", "Fianarantsoa"},
            {"Aigles d'Antsiranana", "Antsiranana"}, {"Tortues de Morondava", "Morondava"},
            {"Sakalava United", "Mahajanga"}, {"Étoiles de Manakara", "Manakara"}};

    private final AppProperties properties;
    private final AppUserRepository userRepository;
    private final TournamentRepository tournamentRepository;
    private final PasswordEncoder passwordEncoder;
    private final TeamService teamService;
    private final TournamentService tournamentService;
    private final RegistrationService registrationService;
    private final MatchGenerationService generationService;
    private final MatchService matchService;

    @Override
    public void run(String... args) {
        if (!properties.seedDemoData()) {
            return;
        }
        try {
            seedUsers();
            if (tournamentRepository.count() == 0) {
                seedCompetitions();
            }
        } catch (RuntimeException e) {
            log.error("Échec du chargement des données de démonstration : l'application démarre sans elles", e);
        }
    }

    private void seedUsers() {
        if (userRepository.count() > 0) {
            return;
        }
        createUser("admin", "admin123", "Administrateur", Role.ADMIN);
        createUser("manager", "manager123", "Responsable de compétition", Role.MANAGER);
        createUser("operator", "operator123", "Opérateur de terrain", Role.OPERATOR);
        createUser("user", "user123", "Spectateur", Role.USER);
        log.info("Comptes de démonstration créés (admin, manager, operator, user)");
    }

    private void createUser(String username, String rawPassword, String fullName, Role role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setFullName(fullName);
        user.setRole(role);
        userRepository.save(user);
    }

    private void seedCompetitions() {
        List<TeamResponse> teams = new ArrayList<>();
        for (String[] data : TEAMS) {
            CreateTeamRequest request = new CreateTeamRequest();
            request.setName(data[0]);
            request.setCity(data[1]);
            request.setCoach("Coach " + data[1]);
            request.setPlayers(playersOf(data[0]));
            teams.add(teamService.create(request));
        }
        LocalDate today = LocalDate.now();

        // 1. championnat en cours, avec les deux premières journées jouées
        TournamentResponse league = tournamentService.create(tournament("Ligue de l'Océan Indien",
                "Football", "Championnat simple à six équipes.", today, today.plusDays(10), "Stade de Mahamasina",
                8, TournamentType.CHAMPIONNAT_SIMPLE));
        tournamentService.openRegistration(league.id());
        teams.subList(0, 6).forEach(t -> registrationService.register(league.id(), t.id()));
        generationService.generate(league.id(), generation(List.of("Terrain A", "Terrain B")));
        tournamentService.start(league.id());
        List<MatchResponse> leagueMatches = matchService
                .search(league.id(), new MatchFilter(), PageRequest.of(0, 100)).getContent();
        int index = 0;
        for (MatchResponse match : leagueMatches) {
            if (match.round() <= 2 && match.status() == MatchStatus.PROGRAMME) {
                matchService.recordResult(match.id(), score(index % 4 + 1, index % 3));
                index++;
            }
        }

        // 2. coupe à 10 équipes (pas une puissance de deux : exempts au premier tour), calendrier généré
        TournamentResponse cup = tournamentService.create(tournament("Coupe de l'Indépendance",
                "Football", "Élimination directe : les mieux classés sont exemptés du premier tour.",
                today.plusDays(20), today.plusDays(30), "Stade Barea", 16, TournamentType.ELIMINATION_DIRECTE));
        tournamentService.openRegistration(cup.id());
        teams.subList(0, 10).forEach(t -> registrationService.register(cup.id(), t.id()));
        generationService.generate(cup.id(), generation(List.of("Stade Barea", "Terrain annexe")));

        // 3. tournoi aller-retour en cours d'inscription
        TournamentResponse youth = tournamentService.create(tournament("Tournoi des Jeunes",
                "Football", "Championnat aller-retour à quatre équipes.", today.plusDays(40), today.plusDays(60),
                "Complexe sportif d'Antsirabe", 6, TournamentType.CHAMPIONNAT_ALLER_RETOUR));
        tournamentService.openRegistration(youth.id());
        teams.subList(6, 10).forEach(t -> registrationService.register(youth.id(), t.id()));

        // 4. brouillon
        tournamentService.create(tournament("Super Coupe", "Basket-ball", null, today.plusDays(90),
                today.plusDays(95), "Palais des Sports", 8, TournamentType.ELIMINATION_DIRECTE));
        log.info("Données de démonstration créées : {} équipes, 4 tournois", teams.size());
    }

    private static CreateTournamentRequest tournament(String name, String sport, String description,
                                                      LocalDate start, LocalDate end, String location,
                                                      int maxTeams, TournamentType type) {
        CreateTournamentRequest request = new CreateTournamentRequest();
        request.setName(name);
        request.setSport(sport);
        request.setDescription(description);
        request.setStartDate(start);
        request.setEndDate(end);
        request.setLocation(location);
        request.setMaxTeams(maxTeams);
        request.setType(type);
        return request;
    }

    private static GenerateMatchesRequest generation(List<String> venues) {
        GenerateMatchesRequest request = new GenerateMatchesRequest();
        request.setVenues(new ArrayList<>(venues));
        return request;
    }

    private static MatchResultRequest score(int home, int away) {
        MatchResultRequest request = new MatchResultRequest();
        request.setHomeScore(home);
        request.setAwayScore(away);
        return request;
    }

    private static List<String> playersOf(String teamName) {
        List<String> players = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            players.add("Joueur " + i + " (" + teamName.split(" ")[0] + ")");
        }
        return players;
    }
}

# Gestion de tournois sportifs — Spring Boot (REST + Thymeleaf)

Moteur de gestion de compétitions : création de tournois, inscription d'équipes, génération automatique du
calendrier (championnat simple / aller-retour, élimination directe avec exempts), saisie des résultats,
classement configurable, arbre d'élimination et statistiques.

Java 21 · Spring Boot 3.3.5 · Spring Data JPA · Spring Security · Thymeleaf · H2 (en mémoire) · Lombok

## Lancer

```bash
mvn spring-boot:run        # http://localhost:8080
mvn test                   # tests unitaires + intégration (MockMvc)
```

Base H2 en mémoire, remplaçable par PostgreSQL/MySQL dans `application.properties`.
Des données de démonstration sont chargées au démarrage (`app.seed-demo-data=false` pour les désactiver).

| Compte   | Mot de passe | Rôle     |
|----------|--------------|----------|
| admin    | admin123     | ADMIN    |
| manager  | manager123   | MANAGER  |
| operator | operator123  | OPERATOR |
| user     | user123      | USER     |

> Comptes de démonstration : à remplacer hors environnement de test.

## Architecture (`com.example.tournament`)

```
config        AppProperties, SecurityConfig, DataInitializer
controller
  ├─ rest     API REST  (/api/**)   ─┐
  └─ web      MVC Thymeleaf         ─┴─► mêmes services
dto
  ├─ request  CreateTournamentRequest, MatchResultRequest… (Bean Validation)
  └─ response TournamentResponse, MatchResponse, BracketResponse… (les entités JPA ne sont jamais exposées)
entity        Tournament, Team, Match, TournamentRegistration, AppUser + énumérations
repository    Spring Data + Specifications (recherche / filtres)
service       interfaces métier
  └─ impl     MatchGenerationServiceImpl, MatchServiceImpl, RegistrationServiceImpl, RankingServiceImpl…
exception     ResourceNotFound / Business / InvalidOperation / Conflict + @RestControllerAdvice + handler MVC
mapper        entité ⇄ DTO
security      authentification, handlers 401/403 JSON
util          RoundRobinPlanner, KnockoutBracketPlanner, StandingsCalculator (logique pure, testable)
```

## Règles métier

- **Statuts** (valeurs d'API, sans accent) : BROUILLON → INSCRIPTION → PROGRAMME → EN_COURS → TERMINE (ou ANNULE).
  Les transitions invalides sont refusées. Rouvrir les inscriptions est impossible tant que le calendrier existe.
- **Championnat** : méthode des tables de Berger, simple ou aller-retour, nombre impair géré (journée de repos).
- **Élimination directe** : nombre de tours calculé automatiquement ; si le nombre d'équipes n'est pas une puissance
  de deux (ex. 10 → tableau de 16, 4 tours), les têtes de série sont exemptées du 1er tour. Match nul impossible :
  tirs au but obligatoires.
- **Génération** unique par tournoi (409 si déjà générée), dans une seule transaction avec verrou pessimiste.
- **Conflits** : une équipe ne peut pas jouer deux matchs simultanés (tous tournois confondus) ; un terrain non plus.
- **Résultats** : vainqueur déterminé automatiquement, équipe qualifiée dans le match suivant, tournoi clôturé quand
  tous les matchs sont joués. Une équipe n'est qualifiée que sur résultat valide ; la correction d'un résultat est
  refusée si le match suivant est déjà joué.
- **Classement** jamais stocké : recalculé à chaque lecture, donc toujours cohérent avec les résultats.
  Barème et critères configurables (points, différence de buts, buts marqués, confrontation directe).
- Un tournoi terminé/annulé n'est plus modifiable ; aucune inscription après le début ; pas de double inscription.

## Rôles

| Rôle     | Droits |
|----------|--------|
| ADMIN    | configuration du classement, suppression de tournoi + tout le reste |
| MANAGER  | gestion métier : tournois, équipes, inscriptions, calendrier |
| OPERATOR | opérations quotidiennes : démarrer un tournoi, saisir les résultats |
| USER     | consultation |

API : HTTP Basic, sans session ni CSRF, erreurs 401/403 en JSON. Interface web : formulaire de connexion + CSRF.

## API REST

| Méthode | URL | Rôle minimum |
|---------|-----|--------------|
| POST | `/api/tournaments` | MANAGER |
| GET | `/api/tournaments?page=0&size=10&sort=startDate,desc&status=EN_COURS&search=…&type=…&sport=…` | USER |
| GET / PUT / DELETE | `/api/tournaments/{id}` | USER / MANAGER / ADMIN |
| POST | `/api/tournaments/{id}/open-registration` · `/cancel` | MANAGER |
| POST | `/api/tournaments/{id}/start` | OPERATOR |
| PUT | `/api/tournaments/{id}/ranking-config` | ADMIN |
| POST / DELETE | `/api/tournaments/{id}/teams` · `/teams/{teamId}` | MANAGER |
| POST / DELETE | `/api/tournaments/{id}/generate-matches` · `/matches` | MANAGER |
| GET | `/api/tournaments/{id}/matches` · `/ranking` · `/bracket` · `/statistics` | USER |
| PUT | `/api/matches/{id}/result` | OPERATOR |
| PUT | `/api/matches/{id}/schedule` | MANAGER |
| CRUD | `/api/teams`, GET `/api/teams/{id}/history` | MANAGER (écriture) |
| GET | `/api/dashboard` | USER |

Codes : 201 + `Location` à la création, 400 (validation, détail par champ), 401, 403, 404, 409 (conflit), 422 (opération invalide).

## Interface Thymeleaf

Tableau de bord, tournois, équipes, génération du calendrier, matchs, saisie des résultats, classement,
arbre d'élimination, statistiques. Les erreurs métier s'affichent en messages lisibles.

package com.example.tournament.util;

import com.example.tournament.entity.RankingCriterion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

/**
 * Calcul du classement d'un championnat à partir des seuls résultats enregistrés.
 * Le classement n'est jamais stocké : il est recalculé à chaque lecture, ce qui garantit
 * qu'une modification de résultat est toujours répercutée.
 */
public final class StandingsCalculator {

    private StandingsCalculator() {
    }

    public record Result(Long homeId, Long awayId, int homeGoals, int awayGoals) {
    }

    public record Rules(int pointsForWin, int pointsForDraw, int pointsForLoss, List<RankingCriterion> criteria) {
    }

    public static final class Row {
        private final Long teamId;
        private final String teamName;
        private int played;
        private int wins;
        private int draws;
        private int losses;
        private int goalsFor;
        private int goalsAgainst;
        private int points;

        Row(Long teamId, String teamName) {
            this.teamId = teamId;
            this.teamName = teamName;
        }

        public Long teamId() {
            return teamId;
        }

        public String teamName() {
            return teamName;
        }

        public int played() {
            return played;
        }

        public int wins() {
            return wins;
        }

        public int draws() {
            return draws;
        }

        public int losses() {
            return losses;
        }

        public int goalsFor() {
            return goalsFor;
        }

        public int goalsAgainst() {
            return goalsAgainst;
        }

        public int goalDifference() {
            return goalsFor - goalsAgainst;
        }

        public int points() {
            return points;
        }
    }

    /**
     * @param teams   équipes engagées (id → nom), y compris celles qui n'ont pas encore joué
     * @param results résultats des matchs terminés
     * @return lignes classées, première place en tête
     */
    public static List<Row> compute(Map<Long, String> teams, List<Result> results, Rules rules) {
        Map<Long, Row> rows = new LinkedHashMap<>();
        teams.forEach((id, name) -> rows.put(id, new Row(id, name)));
        for (Result result : results) {
            apply(rows, result, rules);
        }
        return sort(new ArrayList<>(rows.values()), 0, results, rules);
    }

    private static void apply(Map<Long, Row> rows, Result result, Rules rules) {
        Row home = rows.get(result.homeId());
        Row away = rows.get(result.awayId());
        if (home == null || away == null) {
            return;
        }
        home.played++;
        away.played++;
        home.goalsFor += result.homeGoals();
        home.goalsAgainst += result.awayGoals();
        away.goalsFor += result.awayGoals();
        away.goalsAgainst += result.homeGoals();
        if (result.homeGoals() > result.awayGoals()) {
            home.wins++;
            away.losses++;
            home.points += rules.pointsForWin();
            away.points += rules.pointsForLoss();
        } else if (result.homeGoals() < result.awayGoals()) {
            away.wins++;
            home.losses++;
            away.points += rules.pointsForWin();
            home.points += rules.pointsForLoss();
        } else {
            home.draws++;
            away.draws++;
            home.points += rules.pointsForDraw();
            away.points += rules.pointsForDraw();
        }
    }

    /** Départage récursif : on trie sur le critère courant puis on re-départage chaque groupe d'ex æquo. */
    private static List<Row> sort(List<Row> group, int index, List<Result> results, Rules rules) {
        if (group.size() <= 1) {
            return group;
        }
        List<RankingCriterion> criteria = rules.criteria();
        if (index >= criteria.size()) {
            group.sort(Comparator.comparing((Row r) -> r.teamName().toLowerCase()).thenComparing(Row::teamId));
            return group;
        }

        RankingCriterion criterion = criteria.get(index);
        List<Row> ordered = new ArrayList<>();

        if (criterion == RankingCriterion.HEAD_TO_HEAD) {
            // mini-championnat entre les seules équipes à égalité
            Set<Long> ids = new HashSet<>();
            group.forEach(r -> ids.add(r.teamId()));
            Map<Long, Row> mini = new HashMap<>();
            group.forEach(r -> mini.put(r.teamId(), new Row(r.teamId(), r.teamName())));
            for (Result result : results) {
                if (ids.contains(result.homeId()) && ids.contains(result.awayId())) {
                    apply(mini, result, rules);
                }
            }
            Comparator<Row> byMini = Comparator
                    .comparingInt((Row r) -> mini.get(r.teamId()).points()).reversed()
                    .thenComparing(Comparator.comparingInt((Row r) -> mini.get(r.teamId()).goalDifference()).reversed())
                    .thenComparing(Comparator.comparingInt((Row r) -> mini.get(r.teamId()).goalsFor()).reversed());
            List<Row> sorted = new ArrayList<>(group);
            sorted.sort(byMini);
            int start = 0;
            for (int i = 1; i <= sorted.size(); i++) {
                if (i == sorted.size() || byMini.compare(sorted.get(i - 1), sorted.get(i)) != 0) {
                    ordered.addAll(sort(new ArrayList<>(sorted.subList(start, i)), index + 1, results, rules));
                    start = i;
                }
            }
            return ordered;
        }

        ToIntFunction<Row> key = switch (criterion) {
            case POINTS -> Row::points;
            case GOAL_DIFFERENCE -> Row::goalDifference;
            case GOALS_FOR -> Row::goalsFor;
            case HEAD_TO_HEAD -> throw new IllegalStateException("traité plus haut");
        };
        List<Row> sorted = new ArrayList<>(group);
        sorted.sort(Comparator.comparingInt(key).reversed());
        int start = 0;
        for (int i = 1; i <= sorted.size(); i++) {
            if (i == sorted.size() || key.applyAsInt(sorted.get(i - 1)) != key.applyAsInt(sorted.get(i))) {
                ordered.addAll(sort(new ArrayList<>(sorted.subList(start, i)), index + 1, results, rules));
                start = i;
            }
        }
        return ordered;
    }
}

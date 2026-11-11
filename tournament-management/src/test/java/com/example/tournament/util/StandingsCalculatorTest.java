package com.example.tournament.util;

import com.example.tournament.entity.RankingCriterion;
import com.example.tournament.util.StandingsCalculator.Result;
import com.example.tournament.util.StandingsCalculator.Rules;
import com.example.tournament.util.StandingsCalculator.Row;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StandingsCalculatorTest {

    private static final Rules STANDARD = new Rules(3, 1, 0, List.of(RankingCriterion.POINTS,
            RankingCriterion.GOAL_DIFFERENCE, RankingCriterion.GOALS_FOR, RankingCriterion.HEAD_TO_HEAD));

    private static Map<Long, String> teams(String... names) {
        Map<Long, String> map = new LinkedHashMap<>();
        for (int i = 0; i < names.length; i++) {
            map.put(i + 1L, names[i]);
        }
        return map;
    }

    @Test
    void awardsPointsGoalsAndCountsForWinsDrawsAndLosses() {
        List<Row> rows = StandingsCalculator.compute(teams("A", "B"),
                List.of(new Result(1L, 2L, 3, 1), new Result(2L, 1L, 2, 2)), STANDARD);

        Row a = rows.get(0);
        assertThat(a.teamName()).isEqualTo("A");
        assertThat(a.played()).isEqualTo(2);
        assertThat(a.wins()).isEqualTo(1);
        assertThat(a.draws()).isEqualTo(1);
        assertThat(a.points()).isEqualTo(4);
        assertThat(a.goalsFor()).isEqualTo(5);
        assertThat(a.goalsAgainst()).isEqualTo(3);
        assertThat(a.goalDifference()).isEqualTo(2);
        assertThat(rows.get(1).points()).isEqualTo(1);
    }

    @Test
    void tieOnPointsIsBrokenByGoalDifferenceThenGoalsFor() {
        // B et A ont 4 points et +1 ; B a marqué davantage de buts
        List<Row> rows = StandingsCalculator.compute(teams("A", "B", "C", "D"), List.of(
                new Result(1L, 2L, 2, 0), new Result(3L, 1L, 1, 0), new Result(2L, 3L, 3, 0),
                new Result(4L, 1L, 1, 1), new Result(4L, 2L, 1, 1), new Result(4L, 3L, 1, 1)), STANDARD);

        assertThat(rows).extracting(Row::teamName).containsExactly("B", "A", "C", "D");
    }

    @Test
    void pointsSchemeAndCriteriaAreConfigurable() {
        Rules twoPointsForAWin = new Rules(2, 1, 0, List.of(RankingCriterion.POINTS));

        List<Row> rows = StandingsCalculator.compute(teams("A", "B"), List.of(new Result(2L, 1L, 1, 0)), twoPointsForAWin);

        assertThat(rows.get(0).teamName()).isEqualTo("B");
        assertThat(rows.get(0).points()).isEqualTo(2);
    }

    @Test
    void headToHeadBreaksTiesBetweenTeamsLevelOnEverythingElse() {
        // Z et A : 3 points, même différence et mêmes buts ; Z a gagné la confrontation directe
        Rules rules = new Rules(3, 1, 0, List.of(RankingCriterion.POINTS, RankingCriterion.HEAD_TO_HEAD));
        List<Row> rows = StandingsCalculator.compute(teams("Zed", "Abe", "Cat"), List.of(
                new Result(1L, 2L, 1, 0), new Result(2L, 3L, 1, 0), new Result(3L, 1L, 1, 0)), rules);
        // cycle parfait : tous à 3 points, aucun avantage -> ordre alphabétique stable
        assertThat(rows).extracting(Row::teamName).containsExactly("Abe", "Cat", "Zed");

        List<Row> decided = StandingsCalculator.compute(teams("Zed", "Abe", "Cat"), List.of(
                new Result(1L, 2L, 1, 0), new Result(2L, 3L, 2, 0), new Result(3L, 1L, 0, 0)), rules);
        assertThat(decided.get(0).points()).isGreaterThanOrEqualTo(decided.get(1).points());
    }

    @Test
    void teamsWithoutMatchesAreListedAlphabetically() {
        List<Row> rows = StandingsCalculator.compute(teams("Charlie", "Alpha", "Bravo"), List.of(), STANDARD);

        assertThat(rows).extracting(Row::teamName).containsExactly("Alpha", "Bravo", "Charlie");
        assertThat(rows).allMatch(r -> r.played() == 0 && r.points() == 0);
    }
}

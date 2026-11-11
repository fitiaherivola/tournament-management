package com.example.tournament.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PlannersTest {

    private static List<Long> ids(int n) {
        List<Long> ids = new ArrayList<>();
        for (long i = 1; i <= n; i++) {
            ids.add(i);
        }
        return ids;
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6, 7, 8, 11, 12})
    void roundRobinPlaysEveryPairingExactlyOnce(int teams) {
        var plan = RoundRobinPlanner.plan(ids(teams), false);

        assertThat(plan).hasSize(teams * (teams - 1) / 2);
        Set<String> pairings = new HashSet<>();
        plan.forEach(p -> pairings.add(Math.min(p.homeId(), p.awayId()) + "-" + Math.max(p.homeId(), p.awayId())));
        assertThat(pairings).hasSameSizeAs(plan);
    }

    @ParameterizedTest
    @ValueSource(ints = {3, 4, 5, 8, 10})
    void roundRobinNeverPutsATeamInTwoMatchesOfTheSameRound(int teams) {
        Map<Integer, Set<Long>> byRound = new HashMap<>();
        for (var p : RoundRobinPlanner.plan(ids(teams), true)) {
            Set<Long> busy = byRound.computeIfAbsent(p.round(), r -> new HashSet<>());
            assertThat(busy.add(p.homeId())).isTrue();
            assertThat(busy.add(p.awayId())).isTrue();
        }
    }

    @Test
    void doubleRoundRobinHasReturnLegsWithHomeAndAwaySwapped() {
        var plan = RoundRobinPlanner.plan(ids(4), true);

        assertThat(plan).hasSize(12);
        assertThat(plan.stream().filter(p -> p.leg() == 2)).hasSize(6);
        for (var first : plan.stream().filter(p -> p.leg() == 1).toList()) {
            assertThat(plan).anyMatch(p -> p.leg() == 2
                    && p.homeId().equals(first.awayId()) && p.awayId().equals(first.homeId()));
        }
    }

    @Test
    void roundRobinRejectsInvalidInput() {
        assertThatThrownBy(() -> RoundRobinPlanner.plan(ids(1), false)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> RoundRobinPlanner.plan(List.of(1L, 1L), false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 5, 8, 10, 13, 16, 17})
    void knockoutBracketPlacesEveryTeamOnceAndAddsByes(int teams) {
        int size = KnockoutBracketPlanner.bracketSize(teams);
        var pairs = KnockoutBracketPlanner.firstRoundPairs(teams);

        assertThat(size).isGreaterThanOrEqualTo(teams).isLessThan(Math.max(teams * 2, 3));
        assertThat(Integer.bitCount(size)).isEqualTo(1);
        assertThat(pairs).hasSize(size / 2);
        assertThat(KnockoutBracketPlanner.byeCount(teams)).isEqualTo(size - teams);
        Set<Integer> seeds = new HashSet<>();
        for (Integer[] pair : pairs) {
            assertThat(pair[0] != null || pair[1] != null).as("pas de rencontre vide").isTrue();
            for (Integer seed : pair) {
                if (seed != null) {
                    assertThat(seeds.add(seed)).as("tête de série unique").isTrue();
                }
            }
        }
        assertThat(seeds).hasSize(teams);
    }

    @Test
    void tenTeamsNeedFourRoundsAndSixByes() {
        assertThat(KnockoutBracketPlanner.bracketSize(10)).isEqualTo(16);
        assertThat(KnockoutBracketPlanner.roundCount(10)).isEqualTo(4);
        assertThat(KnockoutBracketPlanner.byeCount(10)).isEqualTo(6);
        assertThat(KnockoutBracketPlanner.seedOrder(8)).containsExactly(1, 8, 4, 5, 2, 7, 3, 6);
    }
}

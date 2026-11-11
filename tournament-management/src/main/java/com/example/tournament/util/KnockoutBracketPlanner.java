package com.example.tournament.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Calculs d'un tableau à élimination directe, y compris quand le nombre d'équipes
 * n'est pas une puissance de deux : le tableau est complété jusqu'à la puissance de
 * deux supérieure et les têtes de série reçoivent un exempt au premier tour.
 */
public final class KnockoutBracketPlanner {

    private KnockoutBracketPlanner() {
    }

    /** Plus petite puissance de deux supérieure ou égale au nombre d'équipes (minimum 2). */
    public static int bracketSize(int teams) {
        if (teams < 2) {
            throw new IllegalArgumentException("Au moins deux équipes sont nécessaires");
        }
        int size = 2;
        while (size < teams) {
            size *= 2;
        }
        return size;
    }

    /** Nombre de tours nécessaires (ex. 10 équipes → tableau de 16 → 4 tours). */
    public static int roundCount(int teams) {
        return Integer.numberOfTrailingZeros(bracketSize(teams));
    }

    /** Nombre d'équipes exemptées au premier tour. */
    public static int byeCount(int teams) {
        return bracketSize(teams) - teams;
    }

    /** Nombre d'équipes engagées au tour {@code round} (1 = premier tour). */
    public static int teamsInRound(int teams, int round) {
        return bracketSize(teams) >> (round - 1);
    }

    /**
     * Ordre standard de placement des têtes de série : 1 et 2 ne peuvent se rencontrer qu'en finale.
     * Ex. taille 8 → [1, 8, 4, 5, 2, 7, 3, 6].
     */
    public static int[] seedOrder(int size) {
        List<Integer> order = new ArrayList<>();
        order.add(1);
        while (order.size() < size) {
            int sum = order.size() * 2 + 1;
            List<Integer> next = new ArrayList<>();
            for (int seed : order) {
                next.add(seed);
                next.add(sum - seed);
            }
            order = next;
        }
        return order.stream().mapToInt(Integer::intValue).toArray();
    }

    /**
     * Rencontres du premier tour. Une valeur {@code null} signifie "pas d'adversaire" (exempt) ;
     * les numéros de série sont compris entre 1 et {@code teams}.
     */
    public static List<Integer[]> firstRoundPairs(int teams) {
        int size = bracketSize(teams);
        int[] order = seedOrder(size);
        List<Integer[]> pairs = new ArrayList<>();
        for (int i = 0; i < size; i += 2) {
            Integer a = order[i] <= teams ? order[i] : null;
            Integer b = order[i + 1] <= teams ? order[i + 1] : null;
            pairs.add(new Integer[] {a, b});
        }
        return pairs;
    }
}

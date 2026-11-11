package com.example.tournament.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Planification d'un championnat (méthode des tables de Berger / "circle method").
 * Logique pure, sans dépendance Spring ni JPA : facile à tester.
 */
public final class RoundRobinPlanner {

    private RoundRobinPlanner() {
    }

    /** Une rencontre planifiée : journée, manche (1 = aller, 2 = retour), identifiants domicile / visiteur. */
    public record Pairing(int round, int leg, Long homeId, Long awayId) {
    }

    /**
     * @param teamIds   identifiants des équipes (au moins 2, sans doublon)
     * @param doubleLeg true pour un championnat aller-retour
     * @return toutes les rencontres, triées par journée
     */
    public static List<Pairing> plan(List<Long> teamIds, boolean doubleLeg) {
        if (teamIds == null || teamIds.size() < 2) {
            throw new IllegalArgumentException("Au moins deux équipes sont nécessaires");
        }
        if (teamIds.stream().distinct().count() != teamIds.size()) {
            throw new IllegalArgumentException("Une équipe ne peut apparaître qu'une fois");
        }

        List<Long> rotation = new ArrayList<>(teamIds);
        if (rotation.size() % 2 != 0) {
            rotation.add(null); // "équipe fantôme" : l'adversaire se repose pendant la journée
        }
        int n = rotation.size();
        int roundsPerLeg = n - 1;

        List<Pairing> firstLeg = new ArrayList<>();
        for (int r = 0; r < roundsPerLeg; r++) {
            for (int i = 0; i < n / 2; i++) {
                Long a = rotation.get(i);
                Long b = rotation.get(n - 1 - i);
                if (a == null || b == null) {
                    continue;
                }
                boolean aHome = (r + i) % 2 == 0; // alternance domicile / extérieur
                firstLeg.add(new Pairing(r + 1, 1, aHome ? a : b, aHome ? b : a));
            }
            rotation.add(1, rotation.remove(n - 1)); // le premier reste fixe, les autres tournent
        }

        List<Pairing> all = new ArrayList<>(firstLeg);
        if (doubleLeg) {
            for (Pairing p : firstLeg) {
                all.add(new Pairing(p.round() + roundsPerLeg, 2, p.awayId(), p.homeId()));
            }
        }
        return all;
    }
}

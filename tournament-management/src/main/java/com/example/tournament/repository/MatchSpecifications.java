package com.example.tournament.repository;

import com.example.tournament.dto.request.MatchFilter;
import com.example.tournament.entity.Match;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class MatchSpecifications {

    private MatchSpecifications() {
    }

    public static Specification<Match> forTournament(Long tournamentId, MatchFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tournament").get("id"), tournamentId));
            if (filter != null) {
                if (filter.getPhase() != null) {
                    predicates.add(cb.equal(root.get("phase"), filter.getPhase()));
                }
                if (filter.getStatus() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.getStatus()));
                }
                if (filter.getRound() != null) {
                    predicates.add(cb.equal(root.get("round"), filter.getRound()));
                }
                if (filter.getTeamId() != null) {
                    predicates.add(cb.or(
                            cb.equal(root.get("homeTeam").get("id"), filter.getTeamId()),
                            cb.equal(root.get("awayTeam").get("id"), filter.getTeamId())));
                }
                if (StringUtils.hasText(filter.getVenue())) {
                    predicates.add(cb.equal(cb.lower(root.get("venue")), filter.getVenue().trim().toLowerCase()));
                }
                if (filter.getFrom() != null) {
                    predicates.add(cb.greaterThanOrEqualTo(root.get("scheduledAt"), filter.getFrom().atStartOfDay()));
                }
                if (filter.getTo() != null) {
                    predicates.add(cb.lessThanOrEqualTo(root.get("scheduledAt"), filter.getTo().atTime(LocalTime.MAX)));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

package com.example.tournament.repository;

import com.example.tournament.dto.request.TournamentFilter;
import com.example.tournament.entity.Tournament;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class TournamentSpecifications {

    private TournamentSpecifications() {
    }

    public static Specification<Tournament> withFilter(TournamentFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filter == null) {
                return cb.conjunction();
            }
            if (StringUtils.hasText(filter.getSearch())) {
                String like = "%" + filter.getSearch().trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), like),
                        cb.like(cb.lower(root.get("sport")), like),
                        cb.like(cb.lower(root.get("location")), like)));
            }
            if (filter.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), filter.getStatus()));
            }
            if (filter.getType() != null) {
                predicates.add(cb.equal(root.get("type"), filter.getType()));
            }
            if (StringUtils.hasText(filter.getSport())) {
                predicates.add(cb.equal(cb.lower(root.get("sport")), filter.getSport().trim().toLowerCase()));
            }
            if (filter.getStartFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startDate"), filter.getStartFrom()));
            }
            if (filter.getStartTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("startDate"), filter.getStartTo()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

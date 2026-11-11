package com.example.tournament.util;

import com.example.tournament.exception.BusinessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Set;

/**
 * Protège les listes paginées : seuls les champs autorisés peuvent servir au tri
 * (un tri sur un champ inconnu devient une erreur 400 claire plutôt qu'une erreur 500).
 */
public final class SortGuard {

    private SortGuard() {
    }

    public static Pageable sanitize(Pageable pageable, Set<String> allowedProperties, Sort defaultSort) {
        Pageable effective = pageable == null || pageable.isUnpaged() ? PageRequest.of(0, 10) : pageable;
        for (Sort.Order order : effective.getSort()) {
            if (!allowedProperties.contains(order.getProperty())) {
                throw new BusinessException("Critère de tri invalide : « " + order.getProperty()
                        + " ». Valeurs autorisées : " + String.join(", ", allowedProperties.stream().sorted().toList()));
            }
        }
        if (effective.getSort().isUnsorted()) {
            return PageRequest.of(effective.getPageNumber(), effective.getPageSize(), defaultSort);
        }
        return effective;
    }
}

package com.example.tournament.util;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/** Reconstruit la chaîne de requête courante (filtres, tri) pour les liens de pagination Thymeleaf. */
public final class QueryStrings {

    private QueryStrings() {
    }

    /**
     * Paramètres de la requête, encodés, sans ceux listés dans {@code excluded} ni les valeurs vides.
     * Le résultat se termine par « &amp; » quand il n'est pas vide, pour pouvoir y ajouter {@code page=n}.
     */
    public static String without(HttpServletRequest request, String... excluded) {
        Set<String> skip = Set.copyOf(Arrays.asList(excluded));
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
            if (skip.contains(entry.getKey())) {
                continue;
            }
            for (String value : entry.getValue()) {
                if (value == null || value.isBlank()) {
                    continue;
                }
                sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                        .append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8))
                        .append('&');
            }
        }
        return sb.toString();
    }
}

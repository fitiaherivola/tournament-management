package com.example.tournament.dto.response;

import java.time.Instant;
import java.util.List;

/** Corps JSON de toutes les erreurs de l'API REST. */
public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldViolation> violations) {

    public record FieldViolation(String field, String message) {
    }
}

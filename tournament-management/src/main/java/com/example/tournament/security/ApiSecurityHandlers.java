package com.example.tournament.security;

import com.example.tournament.dto.response.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Réponses JSON (même format {@link ApiError} que le reste de l'API) pour les erreurs de sécurité :
 * 401 si l'on n'est pas authentifié, 403 si le rôle est insuffisant.
 */
@Component
@RequiredArgsConstructor
public class ApiSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        response.setHeader("WWW-Authenticate", "Basic realm=\"tournament-api\"");
        write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED",
                "Authentification requise : fournissez des identifiants valides");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        write(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "Votre rôle ne vous autorise pas à effectuer cette opération");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                       String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), new ApiError(Instant.now(), status.value(),
                status.getReasonPhrase(), code, message, request.getRequestURI(), List.of()));
    }
}

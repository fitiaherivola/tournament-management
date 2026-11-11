package com.example.tournament.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.net.URI;

/**
 * Gestion des erreurs de l'interface Thymeleaf : l'utilisateur voit toujours un message
 * compréhensible, jamais une trace technique.
 * <ul>
 *   <li>après une action (POST) refusée par une règle métier : retour sur la page d'origine
 *       avec le message affiché dans un bandeau ;</li>
 *   <li>sinon : page d'erreur dédiée avec le bon code HTTP.</li>
 * </ul>
 */
@Slf4j
@ControllerAdvice(basePackages = "com.example.tournament.controller.web")
public class GlobalWebExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ModelAndView notFound(ResourceNotFoundException ex) {
        return errorPage(HttpStatus.NOT_FOUND, "Ressource introuvable", ex.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public Object business(BusinessException ex, HttpServletRequest request) {
        HttpStatus status = ex instanceof ConflictException ? HttpStatus.CONFLICT
                : ex instanceof InvalidOperationException ? HttpStatus.UNPROCESSABLE_ENTITY
                : HttpStatus.BAD_REQUEST;
        String back = backTarget(request);
        if ("POST".equals(request.getMethod()) && back != null) {
            RequestContextUtils.getOutputFlashMap(request).put("error", ex.getMessage());
            return "redirect:" + back;
        }
        return errorPage(status, "Opération impossible", ex.getMessage());
    }

    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class})
    public ModelAndView invalidParameters(Exception ex) {
        return errorPage(HttpStatus.BAD_REQUEST, "Requête invalide",
                "Un des paramètres de la page est incorrect (valeur ou format invalide).");
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ModelAndView concurrency(ConcurrencyFailureException ex) {
        return errorPage(HttpStatus.CONFLICT, "Modification simultanée",
                "Cet élément vient d'être modifié par un autre utilisateur. Rechargez la page puis réessayez.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ModelAndView integrity(DataIntegrityViolationException ex) {
        log.warn("Violation d'intégrité : {}", ex.getMostSpecificCause().getMessage());
        return errorPage(HttpStatus.CONFLICT, "Doublon détecté",
                "L'opération créerait un doublon ou violerait une contrainte de la base.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView accessDenied(AccessDeniedException ex) {
        return errorPage(HttpStatus.FORBIDDEN, "Accès refusé",
                "Votre rôle ne vous autorise pas à effectuer cette opération.");
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView unexpected(Exception ex, HttpServletRequest request) {
        log.error("Erreur inattendue sur {} {}", request.getMethod(), request.getRequestURI(), ex);
        return errorPage(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne",
                "Une erreur inattendue est survenue. Réessayez ou contactez un administrateur.");
    }

    private ModelAndView errorPage(HttpStatus status, String title, String message) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("title", title);
        mav.addObject("message", message);
        return mav;
    }

    /**
     * Chemin (sans hôte) de la page d'origine d'après l'en-tête Referer. On ne garde que le chemin
     * et la requête pour ne jamais rediriger vers un autre site.
     */
    private String backTarget(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = URI.create(referer);
            String path = uri.getRawPath();
            if (path == null || !path.startsWith("/") || path.startsWith("//")) {
                return null;
            }
            return uri.getRawQuery() == null ? path : path + "?" + uri.getRawQuery();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

package com.example.tournament.exception;

/** Opération incompatible avec l'état courant de la ressource (HTTP 422). */
public class InvalidOperationException extends BusinessException {

    public InvalidOperationException(String message) {
        super("INVALID_OPERATION", message);
    }
}

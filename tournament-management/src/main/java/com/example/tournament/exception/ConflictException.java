package com.example.tournament.exception;

/** Conflit avec une donnée existante : doublon, chevauchement, capacité atteinte (HTTP 409). */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }
}

package com.example.tournament.exception;

/** Erreur métier générique (HTTP 400). Les sous-classes précisent la nature de l'erreur. */
public class BusinessException extends RuntimeException {

    private final String code;

    public BusinessException(String message) {
        this("BUSINESS_ERROR", message);
    }

    protected BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}

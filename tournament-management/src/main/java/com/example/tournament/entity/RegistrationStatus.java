package com.example.tournament.entity;

public enum RegistrationStatus {
    REGISTERED("Inscrite"),
    WITHDRAWN("Retirée");

    private final String label;

    RegistrationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

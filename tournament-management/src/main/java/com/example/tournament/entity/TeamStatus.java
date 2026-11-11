package com.example.tournament.entity;

public enum TeamStatus {
    ACTIVE("Active"),
    INACTIVE("Inactive");

    private final String label;

    TeamStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

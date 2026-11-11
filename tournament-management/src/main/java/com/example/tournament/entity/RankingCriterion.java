package com.example.tournament.entity;


public enum RankingCriterion {
    POINTS("Points"),
    GOAL_DIFFERENCE("Différence de buts"),
    GOALS_FOR("Buts marqués"),
    HEAD_TO_HEAD("Confrontation directe");

    private final String label;

    RankingCriterion(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}

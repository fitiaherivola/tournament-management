package com.example.tournament.dto.response;

import com.example.tournament.entity.MatchPhase;

import java.util.List;

public record BracketRound(int round, MatchPhase phase, String label, List<MatchResponse> matches) {
}

package com.example.tournament.mapper;

import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.entity.Match;
import org.springframework.stereotype.Component;

@Component
public class MatchMapper {

    private final TeamMapper teamMapper;

    public MatchMapper(TeamMapper teamMapper) {
        this.teamMapper = teamMapper;
    }

    public MatchResponse toResponse(Match m) {
        return new MatchResponse(
                m.getId(),
                m.getTournament().getId(),
                m.getTournament().getName(),
                m.getPhase(),
                m.getRound(),
                m.getLeg(),
                m.getBracketPosition(),
                teamMapper.toSummary(m.getHomeTeam()),
                teamMapper.toSummary(m.getAwayTeam()),
                m.getScheduledAt(),
                m.getVenue(),
                m.getStatus(),
                m.getHomeScore(),
                m.getAwayScore(),
                m.getHomePenalties(),
                m.getAwayPenalties(),
                teamMapper.toSummary(m.getWinner()),
                m.getNextMatch() != null ? m.getNextMatch().getId() : null);
    }
}

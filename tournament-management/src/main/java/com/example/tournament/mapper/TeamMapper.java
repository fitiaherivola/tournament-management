package com.example.tournament.mapper;

import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.dto.request.UpdateTeamRequest;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.dto.response.TeamSummary;
import com.example.tournament.entity.Team;
import com.example.tournament.entity.TeamStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class TeamMapper {

    public Team toEntity(CreateTeamRequest request) {
        Team team = new Team();
        apply(request, team);
        return team;
    }

    public void update(CreateTeamRequest request, Team team) {
        apply(request, team);
    }

    private void apply(CreateTeamRequest request, Team team) {
        team.setName(request.getName().trim());
        team.setLogo(StringUtils.hasText(request.getLogo()) ? request.getLogo().trim() : null);
        team.setCity(request.getCity().trim());
        team.setCoach(request.getCoach().trim());
        team.getPlayers().clear();
        if (request.getPlayers() != null) {
            request.getPlayers().stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .forEach(team.getPlayers()::add);
        }
        team.setStatus(request.getStatus() != null ? request.getStatus() : TeamStatus.ACTIVE);
    }

    public TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getLogo(), team.getCity(), team.getCoach(),
                new ArrayList<>(team.getPlayers()), team.getStatus());
    }

    public TeamSummary toSummary(Team team) {
        return team == null ? null : new TeamSummary(team.getId(), team.getName());
    }

    /** Pré-remplit un formulaire de modification à partir d'une équipe existante. */
    public UpdateTeamRequest toUpdateRequest(TeamResponse response) {
        UpdateTeamRequest request = new UpdateTeamRequest();
        request.setName(response.name());
        request.setLogo(response.logo());
        request.setCity(response.city());
        request.setCoach(response.coach());
        request.setPlayers(new ArrayList<>(response.players() == null ? List.of() : response.players()));
        request.setStatus(response.status());
        return request;
    }
}

package com.example.tournament.mapper;

import com.example.tournament.dto.request.CreateTournamentRequest;
import com.example.tournament.dto.request.RankingConfigRequest;
import com.example.tournament.dto.request.UpdateTournamentRequest;
import com.example.tournament.dto.response.RegistrationResponse;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.entity.Tournament;
import com.example.tournament.entity.TournamentRegistration;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;

@Component
public class TournamentMapper {

    public Tournament toEntity(CreateTournamentRequest request) {
        Tournament tournament = new Tournament();
        apply(request, tournament);
        return tournament;
    }

    public void update(CreateTournamentRequest request, Tournament tournament) {
        apply(request, tournament);
    }

    private void apply(CreateTournamentRequest request, Tournament tournament) {
        tournament.setName(request.getName().trim());
        tournament.setSport(request.getSport().trim());
        tournament.setDescription(StringUtils.hasText(request.getDescription()) ? request.getDescription().trim() : null);
        tournament.setStartDate(request.getStartDate());
        tournament.setEndDate(request.getEndDate());
        tournament.setLocation(request.getLocation().trim());
        tournament.setMaxTeams(request.getMaxTeams());
        tournament.setType(request.getType());
    }

    public TournamentResponse toResponse(Tournament t, long registeredTeams) {
        return new TournamentResponse(t.getId(), t.getName(), t.getSport(), t.getDescription(),
                t.getStartDate(), t.getEndDate(), t.getLocation(), t.getMaxTeams(), t.getType(), t.getStatus(),
                registeredTeams, t.isMatchesGenerated(), new ArrayList<>(t.getRankingCriteria()),
                t.getPointsForWin(), t.getPointsForDraw(), t.getPointsForLoss());
    }

    public RegistrationResponse toRegistrationResponse(TournamentRegistration r) {
        return new RegistrationResponse(r.getId(), r.getTournament().getId(), r.getTeam().getId(),
                r.getTeam().getName(), r.getTeam().getCity(), r.getRegisteredAt(), r.getWithdrawnAt(), r.getStatus());
    }

    /** Pré-remplit le formulaire de modification. */
    public UpdateTournamentRequest toUpdateRequest(TournamentResponse response) {
        UpdateTournamentRequest request = new UpdateTournamentRequest();
        request.setName(response.name());
        request.setSport(response.sport());
        request.setDescription(response.description());
        request.setStartDate(response.startDate());
        request.setEndDate(response.endDate());
        request.setLocation(response.location());
        request.setMaxTeams(response.maxTeams());
        request.setType(response.type());
        return request;
    }

    /** Pré-remplit le formulaire de configuration du classement. */
    public RankingConfigRequest toRankingConfigRequest(TournamentResponse response) {
        RankingConfigRequest request = new RankingConfigRequest();
        request.setCriteria(new ArrayList<>(response.rankingCriteria()));
        request.setPointsForWin(response.pointsForWin());
        request.setPointsForDraw(response.pointsForDraw());
        request.setPointsForLoss(response.pointsForLoss());
        return request;
    }
}

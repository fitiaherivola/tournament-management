package com.example.tournament.service.impl;

import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.dto.request.TeamFilter;
import com.example.tournament.dto.request.UpdateTeamRequest;
import com.example.tournament.dto.response.MatchResponse;
import com.example.tournament.dto.response.TeamHistoryResponse;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.entity.Match;
import com.example.tournament.entity.MatchStatus;
import com.example.tournament.entity.RegistrationStatus;
import com.example.tournament.entity.Team;
import com.example.tournament.entity.TeamStatus;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.exception.ConflictException;
import com.example.tournament.exception.InvalidOperationException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.MatchMapper;
import com.example.tournament.mapper.TeamMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.RegistrationRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TeamSpecifications;
import com.example.tournament.service.TeamService;
import com.example.tournament.util.SortGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TeamServiceImpl implements TeamService {

    private static final Set<String> SORTABLE = Set.of("id", "name", "city", "coach", "status");

    private final TeamRepository teamRepository;
    private final RegistrationRepository registrationRepository;
    private final MatchRepository matchRepository;
    private final TeamMapper teamMapper;
    private final MatchMapper matchMapper;

    @Override
    @Transactional
    public TeamResponse create(CreateTeamRequest request) {
        String name = request.getName().trim();
        if (teamRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Une équipe nommée « " + name + " » existe déjà");
        }
        return teamMapper.toResponse(teamRepository.save(teamMapper.toEntity(request)));
    }

    @Override
    public TeamResponse get(Long id) {
        return teamMapper.toResponse(find(id));
    }

    @Override
    public Page<TeamResponse> search(TeamFilter filter, Pageable pageable) {
        Pageable effective = SortGuard.sanitize(pageable, SORTABLE, Sort.by("name").and(Sort.by("id")));
        return teamRepository.findAll(TeamSpecifications.withFilter(filter), effective).map(teamMapper::toResponse);
    }

    @Override
    @Transactional
    public TeamResponse update(Long id, UpdateTeamRequest request) {
        Team team = find(id);
        String name = request.getName().trim();
        if (teamRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Une autre équipe est déjà nommée « " + name + " »");
        }
        if (request.getStatus() == TeamStatus.INACTIVE && team.getStatus() != TeamStatus.INACTIVE
                && registrationRepository.existsActiveInTournamentWithStatus(id, List.of(
                TournamentStatus.INSCRIPTION, TournamentStatus.PROGRAMME, TournamentStatus.EN_COURS))) {
            throw new InvalidOperationException(
                    "L'équipe participe à un tournoi en cours ou à venir : elle ne peut pas être désactivée");
        }
        teamMapper.update(request, team);
        return teamMapper.toResponse(team);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Team team = find(id);
        if (registrationRepository.existsByTeamIdAndStatus(id, RegistrationStatus.REGISTERED)) {
            throw new ConflictException("L'équipe « " + team.getName() + " » est inscrite à au moins un tournoi : "
                    + "retirez-la d'abord, ou passez-la en « inactive » pour conserver son historique");
        }
        registrationRepository.deleteAllByTeam(id);
        teamRepository.delete(team);
    }

    @Override
    public TeamHistoryResponse history(Long id) {
        Team team = find(id);
        List<TeamHistoryResponse.Participation> participations = registrationRepository.findHistoryByTeam(id).stream()
                .map(r -> new TeamHistoryResponse.Participation(r.getTournament().getId(), r.getTournament().getName(),
                        r.getTournament().getStatus(), r.getStatus(), r.getRegisteredAt()))
                .toList();

        List<Match> matches = matchRepository.findByTeam(id);
        int wins = 0;
        int draws = 0;
        int losses = 0;
        for (Match m : matches) {
            if (m.getStatus() != MatchStatus.TERMINE) {
                continue;
            }
            if (m.getWinner() == null) {
                draws++;
            } else if (m.getWinner().getId().equals(id)) {
                wins++;
            } else {
                losses++;
            }
        }
        List<MatchResponse> matchResponses = matches.stream().map(matchMapper::toResponse).toList();
        return new TeamHistoryResponse(teamMapper.toResponse(team), wins + draws + losses, wins, draws, losses,
                participations, matchResponses);
    }

    private Team find(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Équipe", id));
    }
}

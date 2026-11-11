package com.example.tournament.service.impl;

import com.example.tournament.dto.response.RegistrationResponse;
import com.example.tournament.dto.response.TeamResponse;
import com.example.tournament.entity.RegistrationStatus;
import com.example.tournament.entity.Team;
import com.example.tournament.entity.TeamStatus;
import com.example.tournament.entity.Tournament;
import com.example.tournament.entity.TournamentRegistration;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.exception.ConflictException;
import com.example.tournament.exception.InvalidOperationException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.TeamMapper;
import com.example.tournament.mapper.TournamentMapper;
import com.example.tournament.repository.RegistrationRepository;
import com.example.tournament.repository.TeamRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.service.MatchGenerationService;
import com.example.tournament.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RegistrationServiceImpl implements RegistrationService {

    private final TournamentRepository tournamentRepository;
    private final TeamRepository teamRepository;
    private final RegistrationRepository registrationRepository;
    private final MatchGenerationService matchGenerationService;
    private final TournamentMapper tournamentMapper;
    private final TeamMapper teamMapper;

    @Override
    @Transactional
    public RegistrationResponse register(Long tournamentId, Long teamId) {
        Tournament tournament = lockTournament(tournamentId);
        ensureRegistrationOpen(tournament);
        if (tournament.isMatchesGenerated()) {
            throw new InvalidOperationException("Le calendrier a déjà été généré : supprimez-le avant d'inscrire "
                    + "une nouvelle équipe");
        }

        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> ResourceNotFoundException.of("Équipe", teamId));
        if (team.getStatus() != TeamStatus.ACTIVE) {
            throw new InvalidOperationException("L'équipe « " + team.getName() + " » est inactive : "
                    + "elle ne peut pas être inscrite à un tournoi");
        }

        Optional<TournamentRegistration> existing =
                registrationRepository.findByTournamentIdAndTeamId(tournamentId, teamId);
        if (existing.isPresent() && existing.get().getStatus() == RegistrationStatus.REGISTERED) {
            throw new ConflictException("L'équipe « " + team.getName() + " » est déjà inscrite à ce tournoi");
        }

        long registered = registrationRepository.countByTournamentIdAndStatus(tournamentId, RegistrationStatus.REGISTERED);
        if (registered >= tournament.getMaxTeams()) {
            throw new ConflictException("Le tournoi est complet (" + tournament.getMaxTeams() + " équipes maximum)");
        }

        // une équipe retirée peut se réinscrire : on réactive la ligne existante (historique conservé)
        TournamentRegistration registration = existing.orElseGet(TournamentRegistration::new);
        registration.setTournament(tournament);
        registration.setTeam(team);
        registration.setRegisteredAt(LocalDateTime.now());
        registration.setWithdrawnAt(null);
        registration.setStatus(RegistrationStatus.REGISTERED);
        return tournamentMapper.toRegistrationResponse(registrationRepository.save(registration));
    }

    @Override
    @Transactional
    public void withdraw(Long tournamentId, Long teamId) {
        Tournament tournament = lockTournament(tournamentId);
        if (tournament.getStatus() != TournamentStatus.INSCRIPTION
                && tournament.getStatus() != TournamentStatus.PROGRAMME) {
            throw new InvalidOperationException("Une équipe ne peut être retirée qu'avant le début de la compétition "
                    + "(statut actuel du tournoi : « " + tournament.getStatus().getLabel() + " »)");
        }
        TournamentRegistration registration = registrationRepository.findByTournamentIdAndTeamId(tournamentId, teamId)
                .filter(r -> r.getStatus() == RegistrationStatus.REGISTERED)
                .orElseThrow(() -> new ResourceNotFoundException("Cette équipe n'est pas inscrite à ce tournoi"));

        if (tournament.isMatchesGenerated()) {
            // le calendrier n'est plus valable sans cette équipe : il sera à régénérer
            matchGenerationService.reset(tournamentId);
        }
        registration.setStatus(RegistrationStatus.WITHDRAWN);
        registration.setWithdrawnAt(LocalDateTime.now());
    }

    @Override
    public List<RegistrationResponse> list(Long tournamentId, boolean includeWithdrawn) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw ResourceNotFoundException.of("Tournoi", tournamentId);
        }
        List<TournamentRegistration> registrations = includeWithdrawn
                ? registrationRepository.findAllByTournament(tournamentId)
                : registrationRepository.findActiveByTournament(tournamentId);
        return registrations.stream().map(tournamentMapper::toRegistrationResponse).toList();
    }

    @Override
    public List<TeamResponse> availableTeams(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw ResourceNotFoundException.of("Tournoi", tournamentId);
        }
        return teamRepository.findAvailableForTournament(tournamentId).stream().map(teamMapper::toResponse).toList();
    }

    private Tournament lockTournament(Long id) {
        return tournamentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tournoi", id));
    }

    private void ensureRegistrationOpen(Tournament tournament) {
        String reason = switch (tournament.getStatus()) {
            case INSCRIPTION -> null;
            case BROUILLON -> "les inscriptions ne sont pas encore ouvertes";
            case PROGRAMME -> "le calendrier a déjà été généré";
            case EN_COURS -> "la compétition a déjà commencé";
            case TERMINE -> "le tournoi est terminé";
            case ANNULE -> "le tournoi est annulé";
        };
        if (reason != null) {
            throw new InvalidOperationException("Inscription impossible : " + reason);
        }
    }
}

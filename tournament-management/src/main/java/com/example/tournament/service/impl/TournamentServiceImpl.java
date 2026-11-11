package com.example.tournament.service.impl;

import com.example.tournament.dto.request.CreateTournamentRequest;
import com.example.tournament.dto.request.RankingConfigRequest;
import com.example.tournament.dto.request.TournamentFilter;
import com.example.tournament.dto.request.UpdateTournamentRequest;
import com.example.tournament.dto.response.TournamentResponse;
import com.example.tournament.entity.RegistrationStatus;
import com.example.tournament.entity.Tournament;
import com.example.tournament.entity.TournamentStatus;
import com.example.tournament.exception.ConflictException;
import com.example.tournament.exception.InvalidOperationException;
import com.example.tournament.exception.ResourceNotFoundException;
import com.example.tournament.mapper.TournamentMapper;
import com.example.tournament.repository.MatchRepository;
import com.example.tournament.repository.RegistrationRepository;
import com.example.tournament.repository.TournamentRepository;
import com.example.tournament.repository.TournamentSpecifications;
import com.example.tournament.service.TournamentService;
import com.example.tournament.util.SortGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TournamentServiceImpl implements TournamentService {

    private static final Set<String> SORTABLE = Set.of(
            "id", "name", "sport", "location", "startDate", "endDate", "maxTeams", "status", "type");

    private final TournamentRepository tournamentRepository;
    private final RegistrationRepository registrationRepository;
    private final MatchRepository matchRepository;
    private final TournamentMapper mapper;

    @Override
    @Transactional
    public TournamentResponse create(CreateTournamentRequest request) {
        Tournament tournament = mapper.toEntity(request);
        tournament.setStatus(TournamentStatus.BROUILLON);
        return toResponse(tournamentRepository.save(tournament));
    }

    @Override
    public TournamentResponse get(Long id) {
        return toResponse(find(id));
    }

    @Override
    public Page<TournamentResponse> search(TournamentFilter filter, Pageable pageable) {
        Pageable effective = SortGuard.sanitize(pageable, SORTABLE,
                Sort.by(Sort.Direction.DESC, "startDate").and(Sort.by("id")));
        return tournamentRepository.findAll(TournamentSpecifications.withFilter(filter), effective)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public TournamentResponse update(Long id, UpdateTournamentRequest request) {
        Tournament tournament = find(id);
        ensureNotFinal(tournament, "modifié");

        long registered = registrationRepository.countByTournamentIdAndStatus(id, RegistrationStatus.REGISTERED);
        if (request.getMaxTeams() < registered) {
            throw new ConflictException("Le nombre maximum d'équipes (" + request.getMaxTeams()
                    + ") est inférieur au nombre d'équipes déjà inscrites (" + registered + ")");
        }
        if (tournament.isMatchesGenerated() && request.getType() != tournament.getType()) {
            throw new InvalidOperationException(
                    "Le type de tournoi ne peut plus être modifié : le calendrier a déjà été généré");
        }
        mapper.update(request, tournament);
        return toResponse(tournament);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Tournament tournament = find(id);
        Set<TournamentStatus> deletable = EnumSet.of(
                TournamentStatus.BROUILLON, TournamentStatus.INSCRIPTION, TournamentStatus.ANNULE);
        if (!deletable.contains(tournament.getStatus())) {
            throw new ConflictException("Un tournoi « " + tournament.getStatus().getLabel()
                    + " » ne peut pas être supprimé : annulez-le d'abord ou conservez-le dans l'historique");
        }
        matchRepository.detachNextMatches(id);
        matchRepository.deleteAllByTournament(id);
        registrationRepository.deleteAllByTournament(id);
        tournamentRepository.delete(tournament);
    }

    @Override
    @Transactional
    public TournamentResponse openRegistration(Long id) {
        Tournament tournament = lock(id);
        if (tournament.isMatchesGenerated()) {
            throw new InvalidOperationException("Le calendrier a déjà été généré : supprimez-le avant de rouvrir "
                    + "les inscriptions");
        }
        tournament.moveTo(TournamentStatus.INSCRIPTION);
        return toResponse(tournament);
    }

    @Override
    @Transactional
    public TournamentResponse start(Long id) {
        Tournament tournament = lock(id);
        if (tournament.getStatus() == TournamentStatus.INSCRIPTION) {
            throw new InvalidOperationException(
                    "Le tournoi ne peut pas démarrer : générez d'abord le calendrier des matchs");
        }
        tournament.moveTo(TournamentStatus.EN_COURS);
        return toResponse(tournament);
    }

    @Override
    @Transactional
    public TournamentResponse cancel(Long id) {
        Tournament tournament = lock(id);
        tournament.moveTo(TournamentStatus.ANNULE);
        return toResponse(tournament);
    }

    @Override
    @Transactional
    public TournamentResponse updateRankingConfig(Long id, RankingConfigRequest request) {
        Tournament tournament = lock(id);
        ensureNotFinal(tournament, "reconfiguré");
        if (!tournament.getType().isChampionship()) {
            throw new InvalidOperationException("Le classement n'existe que pour un championnat");
        }
        if (request.getCriteria().stream().distinct().count() != request.getCriteria().size()) {
            throw new InvalidOperationException("Un critère de classement ne peut apparaître qu'une seule fois");
        }
        if (request.getPointsForWin() < request.getPointsForDraw()
                || request.getPointsForDraw() < request.getPointsForLoss()) {
            throw new InvalidOperationException(
                    "Le barème doit respecter : points de victoire ≥ points de nul ≥ points de défaite");
        }
        tournament.getRankingCriteria().clear();
        tournament.getRankingCriteria().addAll(request.getCriteria());
        tournament.setPointsForWin(request.getPointsForWin());
        tournament.setPointsForDraw(request.getPointsForDraw());
        tournament.setPointsForLoss(request.getPointsForLoss());
        return toResponse(tournament);
    }

    // ---- utilitaires ----

    private Tournament find(Long id) {
        return tournamentRepository.findById(id).orElseThrow(() -> ResourceNotFoundException.of("Tournoi", id));
    }

    private Tournament lock(Long id) {
        return tournamentRepository.findByIdForUpdate(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Tournoi", id));
    }

    private void ensureNotFinal(Tournament tournament, String action) {
        if (tournament.getStatus().isFinal()) {
            throw new InvalidOperationException("Un tournoi « " + tournament.getStatus().getLabel()
                    + " » ne peut plus être " + action);
        }
    }

    private TournamentResponse toResponse(Tournament tournament) {
        long registered = registrationRepository.countByTournamentIdAndStatus(
                tournament.getId(), RegistrationStatus.REGISTERED);
        return mapper.toResponse(tournament, registered);
    }
}

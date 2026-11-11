package com.example.tournament.service;

import com.example.tournament.dto.request.CreateTeamRequest;
import com.example.tournament.dto.request.TeamFilter;
import com.example.tournament.dto.request.UpdateTeamRequest;
import com.example.tournament.dto.response.TeamHistoryResponse;
import com.example.tournament.dto.response.TeamResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TeamService {

    TeamResponse create(CreateTeamRequest request);

    TeamResponse get(Long id);

    Page<TeamResponse> search(TeamFilter filter, Pageable pageable);

    TeamResponse update(Long id, UpdateTeamRequest request);

    void delete(Long id);

    TeamHistoryResponse history(Long id);
}

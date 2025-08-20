package org.com.dungeontalk.domain.world.service;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.world.dto.response.WorldResponse;
import org.com.dungeontalk.domain.world.repository.WorldRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorldService {

    private final WorldRepository worldRepository;

    public List<WorldResponse> getAllWorlds() {
        return worldRepository.findAll().stream()
                .map(WorldResponse::from)
                .collect(Collectors.toList());
    }
}
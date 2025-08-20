package org.com.dungeontalk.domain.worldtype.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.worldtype.dto.request.WorldTypeCreateRequest;
import org.com.dungeontalk.domain.worldtype.dto.request.WorldTypeUpdateRequest;
import org.com.dungeontalk.domain.worldtype.dto.response.WorldTypeResponse;
import org.com.dungeontalk.domain.worldtype.entity.WorldType;
import org.com.dungeontalk.domain.worldtype.repository.WorldTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 세계관 관리 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class WorldTypeService {

    private final WorldTypeRepository worldTypeRepository;

    /**
     * 모든 세계관 조회
     */
    public List<WorldTypeResponse> getAllWorldTypes() {
        return worldTypeRepository.findAllOrderBySortOrder()
                .stream()
                .map(WorldTypeResponse::from)
                .toList();
    }

    /**
     * 활성화된 세계관만 조회
     */
    public List<WorldTypeResponse> getActiveWorldTypes() {
        return worldTypeRepository.findActiveWorldTypesOrderBySortOrder()
                .stream()
                .map(WorldTypeResponse::from)
                .toList();
    }

    /**
     * 특정 세계관 조회
     */
    public WorldTypeResponse getWorldType(Long id) {
        WorldType worldType = worldTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("세계관을 찾을 수 없습니다: " + id));
        return WorldTypeResponse.from(worldType);
    }

    /**
     * 코드로 세계관 조회
     */
    public WorldTypeResponse getWorldTypeByCode(String code) {
        WorldType worldType = worldTypeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("세계관을 찾을 수 없습니다: " + code));
        return WorldTypeResponse.from(worldType);
    }

    /**
     * 세계관 생성
     */
    @Transactional
    public WorldTypeResponse createWorldType(WorldTypeCreateRequest request) {
        // 코드 중복 확인
        if (worldTypeRepository.existsByCode(request.getCode())) {
            throw new IllegalArgumentException("이미 존재하는 코드입니다: " + request.getCode());
        }

        WorldType worldType = WorldType.builder()
                .code(request.getCode())
                .displayName(request.getDisplayName())
                .description(request.getDescription())
                .gameSettings(request.getGameSettings())
                .sortOrder(request.getSortOrder())
                .isActive(true)
                .build();

        WorldType savedWorldType = worldTypeRepository.save(worldType);
        log.info("새로운 세계관이 생성되었습니다: {}", savedWorldType.getCode());

        return WorldTypeResponse.from(savedWorldType);
    }

    /**
     * 세계관 수정
     */
    @Transactional
    public WorldTypeResponse updateWorldType(Long id, WorldTypeUpdateRequest request) {
        WorldType worldType = worldTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("세계관을 찾을 수 없습니다: " + id));

        worldType.update(
                request.getDisplayName(),
                request.getDescription(),
                request.getGameSettings(),
                request.getSortOrder()
        );

        log.info("세계관이 수정되었습니다: {}", worldType.getCode());
        return WorldTypeResponse.from(worldType);
    }

    /**
     * 세계관 활성화/비활성화
     */
    @Transactional
    public WorldTypeResponse toggleWorldTypeStatus(Long id) {
        WorldType worldType = worldTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("세계관을 찾을 수 없습니다: " + id));

        if (worldType.getIsActive()) {
            worldType.deactivate();
            log.info("세계관이 비활성화되었습니다: {}", worldType.getCode());
        } else {
            worldType.activate();
            log.info("세계관이 활성화되었습니다: {}", worldType.getCode());
        }

        return WorldTypeResponse.from(worldType);
    }

    /**
     * 세계관 삭제
     */
    @Transactional
    public void deleteWorldType(Long id) {
        WorldType worldType = worldTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("세계관을 찾을 수 없습니다: " + id));

        worldTypeRepository.delete(worldType);
        log.info("세계관이 삭제되었습니다: {}", worldType.getCode());
    }

    // === 기존 enum 호환성을 위한 메서드들 ===

    /**
     * 모든 활성화된 세계관의 코드 목록 조회 (기존 enum.values() 대체)
     */
    public List<String> getAllActiveWorldTypeCodes() {
        return worldTypeRepository.findActiveWorldTypesOrderBySortOrder()
                .stream()
                .map(WorldType::getCode)
                .toList();
    }

    /**
     * 코드로 WorldType 엔티티 직접 조회 (내부 서비스 간 호출용)
     */
    public WorldType findWorldTypeEntityByCode(String code) {
        return worldTypeRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("세계관을 찾을 수 없습니다: " + code));
    }

    /**
     * 모든 활성화된 WorldType 엔티티 조회 (내부 서비스 간 호출용)
     */
    public List<WorldType> findAllActiveWorldTypeEntities() {
        return worldTypeRepository.findActiveWorldTypesOrderBySortOrder();
    }
}
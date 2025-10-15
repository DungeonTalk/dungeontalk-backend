package org.com.dungeontalk.domain.matching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.worldtype.entity.WorldType;
import org.com.dungeontalk.domain.worldtype.service.WorldTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 기존 enum 방식과 새로운 DB 방식 간의 호환성을 위한 서비스
 * Caffeine 로컬 메모리 캐시가 적용되어 있으므로 별도의 수동 캐싱 불필요
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorldTypeCompatService {

    private final WorldTypeService worldTypeService;

    /**
     * 문자열 코드로 WorldType 엔티티 조회
     */
    public WorldType getWorldTypeByCode(String code) {
        return worldTypeService.findWorldTypeEntityByCode(code);
    }

    /**
     * 모든 활성화된 WorldType 엔티티 조회
     * Caffeine 로컬 캐시로 자동 캐싱됨 (1시간 TTL)
     */
    public List<WorldType> getAllActiveWorldTypes() {
        return worldTypeService.findAllActiveWorldTypeEntities();
    }

    /**
     * 기존 enum.values() 대체 메서드
     */
    public List<WorldType> values() {
        return getAllActiveWorldTypes();
    }

    /**
     * 기존 enum.valueOf() 대체 메서드
     */
    public WorldType valueOf(String code) {
        return getWorldTypeByCode(code);
    }
}
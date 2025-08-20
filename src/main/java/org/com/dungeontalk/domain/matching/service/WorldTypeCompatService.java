package org.com.dungeontalk.domain.matching.service;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.worldtype.entity.WorldType;
import org.com.dungeontalk.domain.worldtype.service.WorldTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 기존 enum 방식과 새로운 DB 방식 간의 호환성을 위한 서비스
 */
@Service
@RequiredArgsConstructor
public class WorldTypeCompatService {

    private final WorldTypeService worldTypeService;
    
    // 캐시된 활성 세계관 목록 (성능 최적화)
    private List<WorldType> cachedActiveWorldTypes;
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION_MS = 30000; // 30초 캐시

    /**
     * 문자열 코드로 WorldType 엔티티 조회
     */
    public WorldType getWorldTypeByCode(String code) {
        return worldTypeService.findWorldTypeEntityByCode(code);
    }

    /**
     * 모든 활성화된 WorldType 엔티티 조회 (캐시된 버전)
     */
    public List<WorldType> getAllActiveWorldTypes() {
        long currentTime = System.currentTimeMillis();
        
        // 캐시가 없거나 만료된 경우에만 DB 조회
        if (cachedActiveWorldTypes == null || (currentTime - lastCacheUpdate) > CACHE_DURATION_MS) {
            cachedActiveWorldTypes = worldTypeService.findAllActiveWorldTypeEntities();
            lastCacheUpdate = currentTime;
        }
        
        return cachedActiveWorldTypes;
    }

    /**
     * 기존 enum.values() 대체 메서드
     */
    public List<WorldType> values() {
        return getAllActiveWorldTypes();
    }
    
    /**
     * 캐시 강제 초기화 (새로운 세계관 추가/삭제 시 호출)
     */
    public void invalidateCache() {
        cachedActiveWorldTypes = null;
        lastCacheUpdate = 0;
    }

    /**
     * 기존 enum.valueOf() 대체 메서드
     */
    public WorldType valueOf(String code) {
        return getWorldTypeByCode(code);
    }
}
package org.com.dungeontalk.domain.matching.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.matching.common.WorldType;
import org.com.dungeontalk.domain.matching.service.MatchingQueueManager;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 매칭 시스템 초기화 설정
 * 서버 시작 시 Redis 큐를 자동으로 초기화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MatchingStartupConfig implements ApplicationRunner {

    private final MatchingQueueManager queueManager;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("🚀 매칭 시스템 초기화 시작...");
        
        try {
            // 모든 세계관 큐 초기화
            for (WorldType worldType : WorldType.values()) {
                queueManager.clearQueue(worldType);
                log.info("✅ {} 큐 초기화 완료", worldType.getDisplayName());
            }
            
            log.info("🎉 매칭 시스템 초기화 완료! 모든 Redis 큐가 정리되었습니다.");
            
        } catch (Exception e) {
            log.error("❌ 매칭 시스템 초기화 중 오류 발생", e);
        }
    }
}
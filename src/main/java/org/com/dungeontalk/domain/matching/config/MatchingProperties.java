package org.com.dungeontalk.domain.matching.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 매칭 도메인 설정 속성
 */
@Getter
@Component
@ConfigurationProperties(prefix = "app.matching")
public class MatchingProperties {

    /**
     * 스레드 풀 설정
     */
    private final ThreadPool threadPool = new ThreadPool();

    /**
     * Redis TTL 설정 (초 단위)
     */
    private final Ttl ttl = new Ttl();

    /**
     * 시간 계산 관련 설정
     */
    private final Timing timing = new Timing();

    @Getter
    public static class ThreadPool {
        /**
         * 기본 스레드 수
         */
        private int corePoolSize = 2;

        /**
         * 최대 스레드 수
         */
        private int maxPoolSize = 10;

        /**
         * 대기 큐 크기
         */
        private int queueCapacity = 50;

        /**
         * 유휴 스레드 유지 시간 (초)
         */
        private int keepAliveSeconds = 60;
    }

    @Getter
    public static class Ttl {
        /**
         * 사용자 상태 TTL (초)
         */
        private long userStatusSeconds = 3600L; // 1시간

        /**
         * 세션 정보 TTL (초)
         */
        private long sessionInfoSeconds = 604800L; // 7일

        /**
         * 매칭 락 타임아웃 (초)
         */
        private long matchingLockTimeoutSeconds = 10L;
    }

    @Getter
    public static class Timing {
        /**
         * 매칭 예상 소요 시간 (초)
         */
        private int estimatedSecondsPerMatch = 30;

        /**
         * 기본 평균 대기 시간 (초)
         */
        private int defaultAverageWaitTimeSeconds = 30;

        /**
         * 정리 작업 간격 (밀리초)
         */
        private long cleanupIntervalMillis = 300000L; // 5분
    }
}
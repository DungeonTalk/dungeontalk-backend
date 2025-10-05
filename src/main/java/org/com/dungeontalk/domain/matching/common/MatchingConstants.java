package org.com.dungeontalk.domain.matching.common;

public final class MatchingConstants {
    
    private MatchingConstants() {
        // 유틸리티 클래스 - 인스턴스화 방지
    }
    
    // 매칭 관련 상수
    public static final int REQUIRED_PARTICIPANTS = 3;
    public static final int MAX_QUEUE_SIZE_PER_WORLD = 100;
    
    // TTL 설정 (초 단위)
    public static final long USER_STATUS_TTL_SECONDS = 3600L; // 1시간
    public static final long SESSION_INFO_TTL_SECONDS = 604800L; // 7일
    
    // Redis 키 프리픽스
    public static final String QUEUE_KEY_PREFIX = "matching:queue:";
    public static final String USER_KEY_PREFIX = "matching:user:";
    public static final String SESSION_KEY_PREFIX = "matching:session:";
    public static final String STATS_KEY_PREFIX = "matching:stats:";
    public static final String LOCK_KEY_PREFIX = "matching:lock:";
    
    // WebSocket 토픽 경로
    public static final String WS_TOPIC_USER_STATUS = "/sub/matching/user/";
    public static final String WS_TOPIC_QUEUE_STATS = "/sub/matching/queue/";
    public static final String WS_DESTINATION_JOIN = "/pub/matching/join";
    public static final String WS_DESTINATION_CANCEL = "/pub/matching/cancel";
    
    // 매칭 처리 설정
    public static final long MATCHING_LOCK_TIMEOUT_SECONDS = 10L;
    public static final long CLEANUP_INTERVAL_MILLISECONDS = 300000L; // 5분
}
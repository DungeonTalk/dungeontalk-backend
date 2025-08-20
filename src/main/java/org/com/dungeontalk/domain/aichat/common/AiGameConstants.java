package org.com.dungeontalk.domain.aichat.common;

/**
 * AI 게임 관련 상수들
 */
public final class AiGameConstants {
    
    private AiGameConstants() {
        // 유틸리티 클래스이므로 인스턴스 생성 방지
    }
    
    // === 게임 시간 관련 상수 ===
    
    /**
     * 기본 게임 목표 시간 (분)
     */
    public static final int DEFAULT_TARGET_DURATION_MINUTES = 15;
    
    /**
     * 기본 게임 목표 시간 (초)
     */
    public static final int DEFAULT_TARGET_DURATION_SECONDS = DEFAULT_TARGET_DURATION_MINUTES * 60;
    
    /**
     * 시간 압박도 계산을 위한 임계값들 (분)
     */
    public static final int TIME_PRESSURE_VERY_URGENT_MINUTES = 3;
    public static final int TIME_PRESSURE_URGENT_MINUTES = 7;
    public static final int TIME_PRESSURE_RELAXED_MINUTES = 2;
    
    // === 게임 플레이 관련 상수 ===
    
    /**
     * 기본 최대 참여자 수
     */
    public static final int DEFAULT_MAX_PARTICIPANTS = 3;
    
    /**
     * 시작 턴 번호
     */
    public static final int INITIAL_TURN_NUMBER = 1;
    
    /**
     * AI 응답 최대 대기 시간 (초)
     */
    public static final int AI_RESPONSE_TIMEOUT_SECONDS = 30;
    
    // === 메시지 관련 상수 ===
    
    /**
     * AI 컨텍스트용 최근 메시지 수
     */
    public static final int AI_CONTEXT_MESSAGE_COUNT = 10;
    
    /**
     * 메시지 최대 길이
     */
    public static final int MAX_MESSAGE_LENGTH = 2000;
}
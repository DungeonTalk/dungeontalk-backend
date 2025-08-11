package org.com.dungeontalk.domain.aichat.common;

public final class AiChatConstants {
    
    private AiChatConstants() {
        // 유틸리티 클래스 인스턴스화 방지
    }
    
    // WebSocket Destinations
    public static final String WEBSOCKET_DESTINATION_PREFIX = "/sub/aichat/room/";
    
    // 턴 메시지 템플릿  
    public static final String TURN_START_MESSAGE_TEMPLATE = "턴 %d 시작! 플레이어들의 행동을 기다립니다.";
    public static final String TURN_END_MESSAGE_TEMPLATE = "턴 %d 완료! 다음 턴을 준비합니다.";
    
    // AI 서비스 기본 설정값
    public static final String DEFAULT_AI_SERVICE_URL = "http://localhost:8001";
    public static final int DEFAULT_AI_SERVICE_TIMEOUT = 30000;
    
    // 캐시/세션 키 접두사
    public static final String AI_GAME_SESSION_PREFIX = "ai_game_session:";
    public static final String AI_GAME_TURN_LOCK_PREFIX = "ai_game_turn_lock:";
    
    // 기본 타임아웃 설정
    public static final int DEFAULT_SESSION_TIMEOUT_SECONDS = 3600; // 1시간
    public static final int DEFAULT_TURN_LOCK_TIMEOUT_SECONDS = 300; // 5분
    
    // 발신자 ID 및 닉네임 상수
    public static final String AI_SENDER_ID = "AI_GM";
    public static final String AI_SENDER_NICKNAME = "던전 마스터";
    public static final String SYSTEM_SENDER_ID = "SYSTEM";
    public static final String SYSTEM_SENDER_NICKNAME = "시스템";
    
    // 메시지 순서 상수
    public static final int TURN_START_MESSAGE_ORDER = 0;      // 턴 시작은 첫 번째
    public static final int TURN_END_MESSAGE_ORDER = 9999;     // 턴 종료는 마지막
    public static final int ERROR_MESSAGE_ORDER = 9998;       // 에러 메시지는 마지막에서 두 번째
    
    // AI 컨텍스트 설정
    public static final int DEFAULT_CONTEXT_MESSAGE_COUNT = 5;  // 기본 컨텍스트 메시지 개수
}
package org.com.dungeontalk.domain.room.common;

/**
 * 통합된 메시지 타입
 * AI 채팅과 일반 채팅의 메시지 타입을 통합
 */
public enum UnifiedMessageType {
    // === 공통 메시지 타입 ===
    /**
     * 사용자가 보낸 일반 메시지
     */
    USER("user", "사용자 메시지"),
    
    /**
     * 시스템이 생성한 메시지 (입장/퇴장 등)
     */
    SYSTEM("system", "시스템 메시지"),
    
    // === AI 게임 전용 메시지 타입 ===
    /**
     * AI가 생성한 응답 메시지
     */
    AI_RESPONSE("ai", "AI 응답"),
    
    /**
     * 게임 상태 변경 메시지 (턴 시작/종료 등)
     */
    GAME_STATE("game_state", "게임 상태"),
    
    /**
     * 게임 액션 메시지 (플레이어 행동)
     */
    GAME_ACTION("game_action", "게임 액션"),
    
    // === 플레이어 채팅 전용 메시지 타입 ===
    /**
     * 다른 플레이어가 보낸 메시지
     */
    OTHER_PLAYER("other", "다른 플레이어"),
    
    /**
     * 접속자 상태 변경 메시지
     */
    PRESENCE("presence", "접속 상태"),
    
    /**
     * 룸 정보 변경 메시지 (정원 변경 등)
     */
    ROOM_INFO("room_info", "룸 정보");

    private final String code;
    private final String displayName;

    UnifiedMessageType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * AI 게임에서 사용되는 메시지 타입인지 확인
     */
    public boolean isAiGameType() {
        return this == AI_RESPONSE || this == GAME_STATE || this == GAME_ACTION;
    }

    /**
     * 플레이어 채팅에서 사용되는 메시지 타입인지 확인
     */
    public boolean isPlayerChatType() {
        return this == OTHER_PLAYER || this == PRESENCE || this == ROOM_INFO;
    }

    /**
     * 공통으로 사용되는 메시지 타입인지 확인
     */
    public boolean isCommonType() {
        return this == USER || this == SYSTEM;
    }

    /**
     * 코드로 메시지 타입 찾기
     */
    public static UnifiedMessageType fromCode(String code) {
        for (UnifiedMessageType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown message type code: " + code);
    }
}
package org.com.dungeontalk.domain.room.common;

/**
 * 통합된 룸 상태
 * AI 게임룸과 플레이어 채팅룸의 상태를 통합
 */
public enum UnifiedRoomStatus {
    // === 공통 상태 ===
    /**
     * 룸이 생성됨 (아직 활성화되지 않음)
     */
    CREATED("created", "생성됨"),
    
    /**
     * 룸이 활성 상태 (사용 가능)
     */
    ACTIVE("active", "활성"),
    
    /**
     * 룸이 일시 정지됨
     */
    PAUSED("paused", "일시정지"),
    
    /**
     * 룸이 완료/종료됨
     */
    COMPLETED("completed", "완료"),
    
    /**
     * 룸에 오류가 발생함
     */
    ERROR("error", "오류"),
    
    // === AI 게임 전용 상태 ===
    /**
     * AI 응답 처리 중
     */
    AI_PROCESSING("ai_processing", "AI 처리중"),
    
    /**
     * 플레이어 턴 대기 중
     */
    WAITING_PLAYER_INPUT("waiting_input", "입력대기"),
    
    /**
     * 게임 종료
     */
    GAME_ENDED("game_ended", "게임종료"),
    
    // === 플레이어 채팅 전용 상태 ===
    /**
     * 채팅 가능 상태
     */
    CHAT_AVAILABLE("chat_available", "채팅가능"),
    
    /**
     * 정원 초과로 입장 불가
     */
    FULL("full", "정원초과"),
    
    /**
     * 비활성 상태 (아무도 접속하지 않음)
     */
    INACTIVE("inactive", "비활성");

    private final String code;
    private final String displayName;

    UnifiedRoomStatus(String code, String displayName) {
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
     * AI 게임에서 사용되는 상태인지 확인
     */
    public boolean isAiGameStatus() {
        return this == AI_PROCESSING || this == WAITING_PLAYER_INPUT || this == GAME_ENDED;
    }

    /**
     * 플레이어 채팅에서 사용되는 상태인지 확인
     */
    public boolean isPlayerChatStatus() {
        return this == CHAT_AVAILABLE || this == FULL || this == INACTIVE;
    }

    /**
     * 공통으로 사용되는 상태인지 확인
     */
    public boolean isCommonStatus() {
        return this == CREATED || this == ACTIVE || this == PAUSED || 
               this == COMPLETED || this == ERROR;
    }

    /**
     * 룸이 사용 가능한 상태인지 확인
     */
    public boolean isUsable() {
        return this == ACTIVE || this == CHAT_AVAILABLE || this == WAITING_PLAYER_INPUT;
    }

    /**
     * 룸이 종료된 상태인지 확인
     */
    public boolean isTerminated() {
        return this == COMPLETED || this == ERROR || this == GAME_ENDED;
    }

    /**
     * 코드로 상태 찾기
     */
    public static UnifiedRoomStatus fromCode(String code) {
        for (UnifiedRoomStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown room status code: " + code);
    }
}
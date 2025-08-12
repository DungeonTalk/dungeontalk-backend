package org.com.dungeontalk.domain.room.common;

/**
 * 룸 타입을 정의하는 열거형
 * AI 게임룸과 플레이어 채팅룸을 구분
 */
public enum RoomType {
    /**
     * AI와 상호작용하는 게임룸
     * - 턴제 게임
     * - AI 응답 대기
     * - 게임 상태 관리
     */
    AI_GAME("ai", "AI 게임룸"),
    
    /**
     * 플레이어간 일반 채팅룸
     * - 실시간 채팅
     * - 다중 사용자
     * - 단순 메시지 교환
     */
    PLAYER_CHAT("chat", "플레이어 채팅룸");

    private final String code;
    private final String displayName;

    RoomType(String code, String displayName) {
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
     * 코드로 RoomType 찾기
     */
    public static RoomType fromCode(String code) {
        for (RoomType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown room type code: " + code);
    }
}
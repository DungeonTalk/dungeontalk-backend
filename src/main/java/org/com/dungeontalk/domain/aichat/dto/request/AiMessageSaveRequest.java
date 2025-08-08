package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.Builder;
import lombok.Getter;

/**
 * AI 메시지 저장을 위한 요청 DTO
 * 매개변수 객체 패턴을 적용하여 saveAiMessage 메서드의 복잡성을 줄입니다.
 */
@Getter
@Builder
public class AiMessageSaveRequest {
    
    private final String aiGameRoomId;
    private final String gameId;
    private final String content;
    private final int turnNumber;
    private final Long responseTime;
    private final String aiSources;
    
    /**
     * 빌더 패턴으로 생성된 객체의 유효성을 검증합니다.
     */
    public void validate() {
        if (aiGameRoomId == null || aiGameRoomId.trim().isEmpty()) {
            throw new IllegalArgumentException("게임방 ID는 필수입니다");
        }
        if (gameId == null || gameId.trim().isEmpty()) {
            throw new IllegalArgumentException("게임 ID는 필수입니다");
        }
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용은 필수입니다");
        }
        if (turnNumber < 1) {
            throw new IllegalArgumentException("턴 번호는 1 이상이어야 합니다");
        }
    }
}
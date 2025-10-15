package org.com.dungeontalk.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.com.dungeontalk.domain.chat.common.Status;

import java.time.Instant;

/**
 * 채팅 세션 데이터 DTO
 * Redis에 JSON 직렬화하여 저장되는 세션 정보
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ChatSessionDto {

    /**
     * 채팅방 ID
     */
    private String roomId;

    /**
     * 회원 ID
     */
    private String memberId;

    /**
     * 회원 닉네임
     */
    private String nickname;

    /**
     * 세션 상태 (ONLINE/OFFLINE)
     */
    private Status status;

    /**
     * 세션 생성 시간 (최초 입장 시간)
     */
    private Instant joinedAt;

    /**
     * 마지막 활동 시간 (메시지 전송, heartbeat 등)
     */
    private Instant lastActivity;

    /**
     * WebSocket 세션 ID (디버깅 및 추적용)
     */
    private String websocketSessionId;
}

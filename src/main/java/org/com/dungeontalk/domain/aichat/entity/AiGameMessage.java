package org.com.dungeontalk.domain.aichat.entity;

import lombok.*;
import org.com.dungeontalk.domain.aichat.common.AiMessageType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * AI 게임 메시지를 저장하는 MongoDB 문서 엔티티
 *
 * 기존 ChatMessage와 분리하여 턴제 게임 메시지 및 AI 응답 히스토리 관리
 */
@Document(collection = "ai_game_messages")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
public class AiGameMessage {

    @Id
    private String id;
    private String aiGameRoomId; // 메시지가 속한 AI 게임방 ID
    private String gameId; //연결된 게임 ID (조회 성능을 위한 중복 저장)
    private String senderId; //메시지 발신자 ID
    private String senderNickname; // 발신자 닉네임 (UI 표시용)
    private String content; // 메시지 내용
    private AiMessageType messageType; // 메시지 타입 (USER, AI, SYSTEM, TURN_START, TURN_END)
    private int turnNumber; //메시지가 속한 턴 번호 (1부터 시작)  파이썬에게 보낼때 활용
    private int messageOrder; //메시지 순서 (같은 턴 내에서의 순서)

    @CreatedDate
    private Instant createdAt;

    /**
     * AI 메시지인지 확인
     */
    public boolean isAiMessage() {
        return this.messageType == AiMessageType.AI;
    }

    /**
     * 사용자 메시지인지 확인
     */
    public boolean isUserMessage() {
        return this.messageType == AiMessageType.USER;
    }

    /**
     * 시스템 메시지인지 확인
     */
    public boolean isSystemMessage() {
        return this.messageType == AiMessageType.SYSTEM ||
               this.messageType == AiMessageType.TURN_START ||
               this.messageType == AiMessageType.TURN_END;
    }
}
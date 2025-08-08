package org.com.dungeontalk.domain.aichat.entity;

import lombok.*;
import org.com.dungeontalk.domain.aichat.common.AiMessageType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * AI 게임 메시지를 저장하는 MongoDB 문서 엔티티
 * 
 * 기존 ChatMessage와 분리하여 턴제 게임 메시지 및 AI 응답 히스토리 관리
 */
@Document(collection = "ai_game_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiGameMessage {

    @Id
    private String id;

    /**
     * 메시지가 속한 AI 게임방 ID
     */
    private String aiGameRoomId;

    /**
     * 연결된 게임 ID (조회 성능을 위한 중복 저장)
     */
    private String gameId;

    /**
     * 메시지 발신자 ID 
     * - 플레이어: PostgreSQL Member 테이블의 ID
     * - AI: "AI_GM" 고정값
     * - 시스템: "SYSTEM" 고정값
     */
    private String senderId;

    /**
     * 발신자 닉네임 (UI 표시용)
     * - 플레이어: Member 테이블의 nickName
     * - AI: "던전 마스터" 고정값
     * - 시스템: "시스템" 고정값
     */
    private String senderNickname;

    /**
     * 메시지 내용
     */
    private String content;

    /**
     * 메시지 타입 (USER, AI, SYSTEM, TURN_START, TURN_END)
     */
    private AiMessageType messageType;

    /**
     * 메시지가 속한 턴 번호 (1부터 시작)
     * - 턴별 히스토리 조회 시 사용
     * - Python AI에 컨텍스트 전달 시 활용
     */
    private int turnNumber;

    /**
     * 메시지 순서 (같은 턴 내에서의 순서)
     * - 플레이어들의 메시지 순서 보장
     * - AI 응답은 항상 마지막
     */
    private int messageOrder;

    /**
     * AI 응답 생성에 걸린 시간 (milliseconds)
     * - AI 메시지에만 적용
     * - 성능 모니터링용
     */
    private Long aiResponseTime;

    /**
     * Python AI 서비스에서 사용된 소스 문서들
     * - RAG에서 참조된 문서 목록
     * - JSON 문자열로 저장
     */
    private String aiSources;

    @CreatedDate
    private LocalDateTime createdAt;

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
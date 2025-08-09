package org.com.dungeontalk.domain.aichat.entity;

import lombok.*;
import org.com.dungeontalk.domain.aichat.common.AiGamePhase;
import org.com.dungeontalk.domain.aichat.common.AiGameStatus;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import jakarta.persistence.EntityListeners;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI 게임방 정보를 저장하는 MongoDB 문서 엔티티
 * 
 * 기존 ChatRoom과 분리하여 AI 게임 전용 메타데이터 관리
 */
@Document(collection = "ai_game_rooms")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Builder
public class AiGameRoom {

    @Id
    private String id;

    /**
     * 연결된 게임 ID (PostgreSQL Game 테이블과 연동)
     */
    private String gameId;

    /**
     * AI 게임방 이름
     */
    private String roomName;

    /**
     * AI 게임방 설명 (선택사항)
     */
    private String description;

    /**
     * 게임방 상태 (생성됨, 활성, 일시정지, 완료, 에러)
     */
    private AiGameStatus status;

    /**
     * 현재 게임 진행 단계 (대기, 턴입력, AI응답, 종료)
     */
    private AiGamePhase currentPhase;

    /**
     * 현재 턴 번호 (1부터 시작)
     */
    @Builder.Default
    private int currentTurn = 1;

    /**
     * 최대 참여 가능 인원 (기본 3명)
     */
    @Builder.Default
    private int maxParticipants = 3;

    /**
     * 현재 참여중인 플레이어 ID 목록 (PostgreSQL Member 테이블의 ID)
     */
    private List<String> participants;

    /**
     * 게임 설정 (TRPG 세계관, 난이도 등 - JSON 형태 저장 가능)
     */
    private String gameSettings;

    /**
     * 마지막 활동 시간 (플레이어 입력 또는 AI 응답 시간)
     */
    private LocalDateTime lastActivity;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    /**
     * 게임방이 활성 상태인지 확인
     */
    public boolean isActive() {
        return this.status == AiGameStatus.ACTIVE;
    }

    /**
     * 새로운 플레이어가 입장 가능한지 확인
     */
    public boolean canJoin() {
        return this.status == AiGameStatus.CREATED && 
               this.participants != null && 
               this.participants.size() < this.maxParticipants;
    }

    /**
     * 현재 참여 인원 수 반환
     */
    public int getCurrentParticipantCount() {
        return this.participants != null ? this.participants.size() : 0;
    }
}
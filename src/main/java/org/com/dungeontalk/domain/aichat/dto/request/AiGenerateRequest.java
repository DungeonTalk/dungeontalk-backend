package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * AI 응답 생성 요청 DTO
 * 프론트엔드에서 AI 응답을 직접 요청할 때 사용
 */
@Getter
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class AiGenerateRequest {
    private String gameId;
    private String currentUser;
    private String currentMessage;
    private int turnNumber;
    private Long gameStartTime;  // 게임 시작 시간
    private Integer targetDuration;  // 목표 시간 (분)
    private Object characterStats;  // 캐릭터 스탯 정보

}
package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * AI 응답 생성 요청 DTO
 * 프론트엔드에서 AI 응답을 직접 요청할 때 사용
 */
@Getter
@Setter
public class AiGenerateRequest {
    private String gameId;
    private String currentUser;
    private String currentMessage;
    private int turnNumber;
}
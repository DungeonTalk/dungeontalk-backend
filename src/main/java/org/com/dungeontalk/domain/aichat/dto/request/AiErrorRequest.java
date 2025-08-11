package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * AI 응답 생성 실패 시 사용하는 에러 요청 DTO
 */
@Getter
@Setter
public class AiErrorRequest {
    private String gameId;
    private int turnNumber;
    private String errorMessage;
    private String errorCode;
}
package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

/**
 * AI 응답 생성 실패 시 사용하는 에러 요청 DTO
 */
@Getter
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class AiErrorRequest {
    private String gameId;
    private int turnNumber;
    private String errorMessage;
    private String errorCode;
}
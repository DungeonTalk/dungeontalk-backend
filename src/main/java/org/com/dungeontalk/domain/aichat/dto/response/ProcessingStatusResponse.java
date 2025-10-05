package org.com.dungeontalk.domain.aichat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 응답 처리 상태 응답 DTO
 */
@Getter
@AllArgsConstructor
public class ProcessingStatusResponse {
    private final String roomId;
    private final boolean isProcessing;
    private final boolean isSessionValid;
}
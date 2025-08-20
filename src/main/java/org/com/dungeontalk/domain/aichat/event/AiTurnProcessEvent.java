package org.com.dungeontalk.domain.aichat.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.aichat.dto.request.AiGenerateRequest;

/**
 * AI 턴 처리 이벤트
 */
@Getter
@RequiredArgsConstructor
public class AiTurnProcessEvent {
    private final String aiGameRoomId;
    private final AiGenerateRequest aiRequest;
}
package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * Python AI 서비스에서 생성된 응답을 받는 DTO
 * AI 응답을 저장하고 WebSocket으로 브로드캐스트할 때 사용
 */
@Getter
@Setter
public class AiResponseRequest {
    private String gameId;
    private String content;
    private int turnNumber;
    private Long responseTime;
    private String aiSources;
}
package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ContextMessage {
    private String messageType;
    private String senderNickname;
    private String content;
    private int turnNumber;
    private int messageOrder;
}
package org.com.dungeontalk.domain.chat.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ChatMessageResponse {
    private String id;
    private String roomId;
    private String senderId;
    private String senderNickname;      // PostgreSQL에서 조회된 닉네임
    private String message;
    private LocalDateTime createdAt;
}

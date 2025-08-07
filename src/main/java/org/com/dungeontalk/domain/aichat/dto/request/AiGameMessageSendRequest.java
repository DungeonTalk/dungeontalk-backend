package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.aichat.common.AiMessageType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Getter
@Setter
public class AiGameMessageSendRequest {

    @NotBlank(message = "AI 게임방 ID는 필수입니다")
    private String aiGameRoomId;

    @NotBlank(message = "게임 ID는 필수입니다")
    private String gameId;

    @NotBlank(message = "발신자 ID는 필수입니다")
    private String senderId;

    @NotBlank(message = "발신자 닉네임은 필수입니다")
    private String senderNickname;

    @NotBlank(message = "메시지 내용은 필수입니다")
    private String content;

    @NotNull(message = "메시지 타입은 필수입니다")
    private AiMessageType messageType;

    @Positive(message = "턴 번호는 1 이상이어야 합니다")
    private int turnNumber;

    private int messageOrder;
}
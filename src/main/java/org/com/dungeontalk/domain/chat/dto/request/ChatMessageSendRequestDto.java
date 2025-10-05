package org.com.dungeontalk.domain.chat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;
import org.com.dungeontalk.domain.chat.common.MessageType;
import org.com.dungeontalk.domain.chat.validation.message.ValidChatMessage;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Jacksonized
@ValidChatMessage
public class ChatMessageSendRequestDto {
    @Schema(description = "클라이언트 생성 UUID v7", example = "018f9b17-c0a3-7a02-a5a4-4f3c1adf5b89")
    private String messageId;

    @NotBlank
    @Size(max = 120)
    @Schema(description = "채팅방 ID", example = "018f9b17-c0a3-7a02-a5a4-4f3c1adf5b89")
    private String roomId;

    @NotBlank
    @Size(max = 80)
    @Schema(description = "발신자 ID", example = "018f9b17-c0a3-7a02-a5a4-4f3c1adf5b89")
    private String senderId;

    @Size(max = 40)
    @Schema(description = "발신자 닉네임(선택, 서버가 DB에서 최종 매핑)", example = "SlayerKim")
    private String senderNickname;

    @Size(max = 80)
    @Schema(description = "수신자 ID(귓속말 등 선택)", example = "user-77")
    private String receiverId;

    @Size(max = 2000)
    @Schema(description = "메시지 본문", example = "안녕하세요!")
    private String content;

    @NotNull
    @Schema(description = "메시지 타입", example = "TALK", allowableValues = {"JOIN","TALK","LEAVE","PRESENCE"})
    private MessageType type;

}

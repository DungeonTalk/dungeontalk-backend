package org.com.dungeontalk.domain.aichat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "AI 게임방 참가 요청")
@Getter
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class AiGameRoomJoinRequest {

    @Schema(description = "AI 게임방 ID", example = "room-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "AI 게임방 ID는 필수입니다")
    private String aiGameRoomId;

    @Schema(description = "참여자 ID", example = "user-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "참여자 ID는 필수입니다")
    private String participantId;

    @Schema(description = "참여자 닉네임", example = "플레이어1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "참여자 닉네임은 필수입니다")
    private String participantNickname;
}
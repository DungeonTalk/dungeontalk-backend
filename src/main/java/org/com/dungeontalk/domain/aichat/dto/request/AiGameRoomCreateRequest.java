package org.com.dungeontalk.domain.aichat.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "AI 게임방 생성 요청")
@Getter
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class AiGameRoomCreateRequest {

    @Schema(description = "게임 ID", example = "game-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "게임 ID는 필수입니다")
    private String gameId;

    @Schema(description = "게임방 이름", example = "즐거운 던전 탐험", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "게임방 이름은 필수입니다")
    private String roomName;

    @Schema(description = "최대 참여자 수", example = "3", minimum = "1", maximum = "4")
    @Min(value = 1, message = "최대 참여자는 최소 1명 이상이어야 합니다")
    @Max(value = 4, message = "최대 참여자는 4명을 초과할 수 없습니다")
    @Builder.Default
    private int maxParticipants = 3;

    @Schema(description = "게임 설정 (JSON 형태)", example = "{\"difficulty\": \"normal\", \"theme\": \"fantasy\"}")
    private String gameSettings;

    @Schema(description = "게임방 생성자 ID", example = "user-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "생성자 ID는 필수입니다")
    private String creatorId;
}
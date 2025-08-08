package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class AiGameRoomCreateRequest {

    @NotBlank(message = "게임 ID는 필수입니다")
    private String gameId;

    @NotBlank(message = "게임방 이름은 필수입니다")
    private String roomName;

    private String description;

    @Min(value = 1, message = "최대 참여자는 최소 1명 이상이어야 합니다")
    @Max(value = 3, message = "최대 참여자는 3명을 초과할 수 없습니다")
    private int maxParticipants = 3;

    private String gameSettings;

    @NotBlank(message = "생성자 ID는 필수입니다")
    private String creatorId;
}
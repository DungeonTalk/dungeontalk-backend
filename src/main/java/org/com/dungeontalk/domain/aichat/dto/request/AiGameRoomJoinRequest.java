package org.com.dungeontalk.domain.aichat.dto.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class AiGameRoomJoinRequest {

    @NotBlank(message = "AI 게임방 ID는 필수입니다")
    private String aiGameRoomId;

    @NotBlank(message = "참여자 ID는 필수입니다")
    private String participantId;

    @NotBlank(message = "참여자 닉네임은 필수입니다")
    private String participantNickname;
}
package org.com.dungeontalk.domain.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

@Schema(description = "룸 참여/퇴장 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMemberRequest {

    @Schema(description = "회원 ID", example = "user-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "사용자 ID는 필수입니다")
    private String memberId;
    
    @Schema(description = "닉네임", example = "플레이어1")
    private String nickname;  // 선택사항
    
    @Schema(description = "입장/퇴장 메시지", example = "안녕하세요!")
    private String message;   // 입장/퇴장 메시지 (선택사항)
}
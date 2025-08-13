package org.com.dungeontalk.domain.room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * 룸 참여/퇴장 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoomMemberRequest {

    @NotBlank(message = "사용자 ID는 필수입니다")
    private String memberId;
    
    // 나중에 확장 가능한 필드들
    private String nickname;  // 선택사항
    private String message;   // 입장/퇴장 메시지 (선택사항)
}
package org.com.dungeontalk.domain.matching.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "게임 매칭 참가 요청")
@Getter
@Builder
@Jacksonized
@NoArgsConstructor
@AllArgsConstructor
public class MatchingJoinRequest {

    @Schema(description = "회원 ID", example = "user-12345", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "회원 ID는 필수입니다")
    private String memberId;

    @Schema(description = "게임 세계관 코드", example = "FANTASY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "세계관 선택은 필수입니다")
    private String worldTypeCode;
}
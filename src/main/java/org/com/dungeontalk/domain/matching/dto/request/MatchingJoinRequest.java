package org.com.dungeontalk.domain.matching.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.jackson.Jacksonized;
import org.com.dungeontalk.domain.matching.common.WorldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

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

    @Schema(description = "게임 세계관 타입", example = "FANTASY", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "세계관 선택은 필수입니다")
    private WorldType worldType;
}
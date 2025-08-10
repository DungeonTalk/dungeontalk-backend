package org.com.dungeontalk.domain.matching.dto.request;

import lombok.Getter;
import lombok.Setter;
import org.com.dungeontalk.domain.matching.common.WorldType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Getter
@Setter
public class MatchingJoinRequest {

    @NotBlank(message = "사용자 ID는 필수입니다")
    private String userId;

    @NotNull(message = "세계관 선택은 필수입니다")
    private WorldType worldType;
}
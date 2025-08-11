package org.com.dungeontalk.domain.matching.dto.request;

import lombok.Getter;
import lombok.Setter;

import jakarta.validation.constraints.NotBlank;

@Getter
@Setter
public class MatchingCancelRequest {

    @NotBlank(message = "회원 ID는 필수입니다")
    private String memberId;
}
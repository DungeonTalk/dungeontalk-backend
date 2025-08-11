package org.com.dungeontalk.domain.auth.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class JwtTokenResponse {
    private String accessToken;
    private String refreshToken;
}

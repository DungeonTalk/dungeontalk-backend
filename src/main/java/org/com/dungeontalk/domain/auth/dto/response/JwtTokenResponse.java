package org.com.dungeontalk.domain.auth.dto.response;

import lombok.Getter;

@Getter
public class JwtTokenResponse {
    private String AccessToken;
    private String RefreshToken;
}

package org.com.dungeontalk.domain.auth.dto.response;

import lombok.Getter;

@Getter
public class RefreshTokenResponse {

    private String accessToken;

    public RefreshTokenResponse(String accessToken) {
        this.accessToken = accessToken;
    }

}

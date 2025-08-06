package org.com.dungeontalk.domain.auth.dto.response;

public record AuthLoginResponse(
        String memberId,
        String accessToken,
        String refreshToken
) {
}

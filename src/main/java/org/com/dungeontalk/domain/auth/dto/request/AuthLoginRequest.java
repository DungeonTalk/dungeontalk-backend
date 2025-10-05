package org.com.dungeontalk.domain.auth.dto.request;

public record AuthLoginRequest(
        String name,
        String password
) {
}

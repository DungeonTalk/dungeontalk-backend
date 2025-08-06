package org.com.dungeontalk.domain.member.dto.response;

public record RegisterResponse(
        String id,
        String name,
        String nickName
) {
}

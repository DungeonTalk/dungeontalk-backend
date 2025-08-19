package org.com.dungeontalk.domain.member.dto.response;

public record UserWithCharacterInfoResponse(
        String nickname,
        Meta meta
) {
    public record Meta(
            boolean isExistCharacter
    ) {}

    public static UserWithCharacterInfoResponse of(String nickname, boolean isExistCharacter) {
        return new UserWithCharacterInfoResponse(
                nickname,
                new Meta(isExistCharacter)
        );
    }
}
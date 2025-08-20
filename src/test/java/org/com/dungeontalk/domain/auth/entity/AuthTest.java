package org.com.dungeontalk.domain.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import org.com.dungeontalk.domain.member.entity.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AuthTest {

    @Test
    @DisplayName("순수 POJO 테스트 - Lombok Builder/Getter/Setter가 예상대로 동작한다.")
    void builderAndAccessors() {
        Member member = Member.builder()
            .id("member-1")           // 프로젝트 Member 엔티티에 맞춰 조정하세요
            .name("alice")
            .nickName("Ali")
            .password("ENC")
            .build();

        Auth auth = Auth.builder()
            .member(member)
            .email("alice@example.com")
            .tokenType("bearer")
            .accessToken("access.jwt")
            .refreshToken("refresh.jwt")
            .build();

        assertThat(auth.getMember()).isEqualTo(member);
        assertThat(auth.getEmail()).isEqualTo("alice@example.com");
        assertThat(auth.getTokenType()).isEqualTo("bearer");
        assertThat(auth.getAccessToken()).isEqualTo("access.jwt");
        assertThat(auth.getRefreshToken()).isEqualTo("refresh.jwt");

        auth.setAccessToken(null);
        assertThat(auth.getAccessToken()).isNull();
    }

}
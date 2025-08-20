package org.com.dungeontalk.domain.auth.manager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.com.dungeontalk.domain.auth.dto.request.AuthLoginRequest;
import org.com.dungeontalk.domain.auth.dto.response.JwtTokenResponse;
import org.com.dungeontalk.domain.auth.entity.Auth;
import org.com.dungeontalk.domain.auth.repository.AuthRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.exception.customException.MemberException;
import org.com.dungeontalk.global.security.JwtProvider;
import org.com.dungeontalk.global.security.JwtRedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ActualLoginManagerTest {

    @InjectMocks
    private ActualLoginManager manager;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private JwtRedisService jwtRedisService;

    @Test
    @DisplayName("validateMember: 사용자 존재하고 비밀번호가 일치하면 Member를 반환")
    void validateMember_success() {
        AuthLoginRequest req = new AuthLoginRequest("alice", "pw");
        Member m = Member.builder()
            .id("m1")
            .name("alice")
            .nickName("Ali")
            .password("1234")
            .build();

        when(memberRepository.findByName("alice")).thenReturn(Optional.of(m));
        when(passwordEncoder.matches("pw", "ENC")).thenReturn(true);

        Member out = manager.validateMember(req);
        assertThat(out).isSameAs(m);
    }

    @Test
    @DisplayName("validateMember: 사용자가 존재하지 않으면 MemberException 를 발생시킨다.")
    void validateMember_notFound() {
        when(memberRepository.findByName("ghost")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> manager.validateMember(new AuthLoginRequest("ghost","pw")))
            .isInstanceOf(MemberException.class);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("validateMember: 비밀번호가 일치하지 않으면 MemberException 를 발생시킨다.")
    void validateMember_badPassword() {
        Member m = Member.builder()
            .id("m1")
            .name("alice")
            .password("1234")
            .build();

        when(memberRepository.findByName("alice")).thenReturn(Optional.of(m));
        when(passwordEncoder.matches("pw","1234")).thenReturn(false);

        assertThatThrownBy(() -> manager.validateMember(new AuthLoginRequest("alice","pw")))
            .isInstanceOf(MemberException.class);
    }

    @Test
    @DisplayName("generateToken: provider 호출로 accessToken/refreshToken 을 발급한다.")
    void generateToken() {
        Member member = Member.builder()
            .id("m1")
            .name("alice")
            .nickName("Ali")
            .build();

        when(jwtProvider.generateAccessToken("m1","alice","Ali")).thenReturn("access.jwt");
        when(jwtProvider.generateRefreshToken("m1")).thenReturn("refresh.jwt");

        JwtTokenResponse res = manager.generateToken(member);
        assertThat(res.getAccessToken()).isEqualTo("access.jwt");
        assertThat(res.getRefreshToken()).isEqualTo("refresh.jwt");
    }

    @Test
    @DisplayName("updateMemberRefreshToken: 기존 Auth 존재 시 refreshToken 교체 + 세션 Redis에 저장")
    void updateMemberRefreshToken_existing() {
        Member member = Member.builder()
            .id("m1")
            .name("alice")
            .build();
        JwtTokenResponse jwt = new JwtTokenResponse("A","R");
        Auth existing = Auth.builder()
            .member(member)
            .refreshToken("old")
            .build();

        when(authRepository.findByMember(member)).thenReturn(Optional.of(existing));

        manager.updateMemberRefreshToken(member, jwt);

        assertThat(existing.getAccessToken()).isNull();
        assertThat(existing.getRefreshToken()).isEqualTo("R");
        verify(authRepository).save(existing);
        verify(jwtRedisService).saveRefreshTokenToSessionRedis("m1","R");
    }

    @Test
    @DisplayName("updateMemberRefreshToken: 기존의 Auth가 없으면 신규 생성 및 저장 + 세션 Redis 저장")
    void updateMemberRefreshToken_new() {
        Member member = Member.builder()
            .id("m1")
            .name("alice")
            .build();

        JwtTokenResponse jwt = new JwtTokenResponse("A","R");

        when(authRepository.findByMember(member)).thenReturn(Optional.empty());

        manager.updateMemberRefreshToken(member, jwt);

        ArgumentCaptor<Auth> cap = ArgumentCaptor.forClass(Auth.class);
        verify(authRepository).save(cap.capture());
        Auth saved = cap.getValue();
        assertThat(saved.getMember()).isEqualTo(member);
        assertThat(saved.getRefreshToken()).isEqualTo("R");
        assertThat(saved.getTokenType()).isEqualTo("bearer");
        verify(jwtRedisService).saveRefreshTokenToSessionRedis("m1","R");
    }



}
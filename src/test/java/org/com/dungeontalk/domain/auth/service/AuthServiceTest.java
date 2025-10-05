package org.com.dungeontalk.domain.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.com.dungeontalk.domain.auth.dto.request.AuthLoginRequest;
import org.com.dungeontalk.domain.auth.dto.response.JwtTokenResponse;
import org.com.dungeontalk.domain.auth.dto.response.TokenResponse;
import org.com.dungeontalk.domain.auth.entity.Auth;
import org.com.dungeontalk.domain.auth.manager.ActualLoginManager;
import org.com.dungeontalk.domain.auth.manager.AuthRedisManager;
import org.com.dungeontalk.domain.auth.manager.BruteForceManager;
import org.com.dungeontalk.domain.auth.manager.CookieManager;
import org.com.dungeontalk.domain.auth.repository.AuthRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.MemberException;
import org.com.dungeontalk.global.security.JwtProvider;
import org.com.dungeontalk.global.security.JwtRedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Spy
    @InjectMocks
    private AuthService service;

    @Mock
    private AuthRepository authRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private JwtRedisService jwtRedisService;

    @Mock
    private AuthRedisManager authRedisManager;

    @Mock
    private CookieManager cookieManager;

    @Mock
    private BruteForceManager bruteForceManager;

    @Mock
    private ActualLoginManager actualLoginManager;

    @Mock
    private HttpServletRequest req;

    /**
     * 더 깔끔하게: actualLogin을 직접 호출시키는 것이므로, 아래처럼 stub:
     * 실제 호출: TokenResponse res = actualLogin(request);
     * 검증할 것은 bruteForce 호출 흐름. 따라서 actualLogin만 스파이로 감싼 별도 인스턴스로 테스트하는 게 깔끔.
     */
    @Test
    @DisplayName("login: preCheck → actualLogin → loginSucceeded 순서로 호출되고 토큰 반환")
    void login_success() throws InterruptedException {
        AuthLoginRequest request = new AuthLoginRequest("alice","pw");
        TokenResponse tokenRes = new TokenResponse("A","R");

        // actualLogin만 stub 해서 내부 호출을 막고 결과만 돌려준다
        doReturn(tokenRes).when(service).actualLogin(request);

        TokenResponse response = service.login(request, req);

        // 호출 흐름 검증
        verify(bruteForceManager).preCheck("alice", req);
        verify(service).actualLogin(request);
        verify(bruteForceManager).loginSucceeded("alice");

        assertThat(response.getAccessToken()).isEqualTo("A");
        assertThat(response.getRefreshToken()).isEqualTo("R");
    }

    @Test
    @DisplayName("login: 실제 actualLogin 경로 태우기 (매니저를 any()로 스텁) + ArgumentCaptor로 값 검증")
    void login_success_flow() throws Exception {
        AuthLoginRequest request = new AuthLoginRequest("alice", "pw");
        Member member = Member.builder()
            .id("m1")
            .name("alice")
            .nickName("Ali")
            .build();

        // actualLogin 내부에서의 3단계 호출 스텁
        when(actualLoginManager.validateMember(request)).thenReturn(member);
        when(actualLoginManager.generateToken(member))
            .thenReturn(new JwtTokenResponse("A", "R"));

        // 핵심: void 메서드는 any()로 느슨하게 스텁 (동일 인스턴스 강제 X)
        doNothing().when(actualLoginManager).updateMemberRefreshToken(eq(member), any(JwtTokenResponse.class));

        TokenResponse out = service.login(request, req);

        // 흐름 검증
        verify(bruteForceManager).preCheck("alice", req);
        verify(bruteForceManager).loginSucceeded("alice");

        // 반환값 검증
        assertThat(out.getAccessToken()).isEqualTo("A");
        assertThat(out.getRefreshToken()).isEqualTo("R");

        // 호출 시 전달된 실제 토큰 값도 검증하고 싶다면 ArgumentCaptor 사용
        ArgumentCaptor<JwtTokenResponse> tokenCaptor = ArgumentCaptor.forClass(JwtTokenResponse.class);
        verify(actualLoginManager).updateMemberRefreshToken(eq(member), tokenCaptor.capture());
        JwtTokenResponse passed = tokenCaptor.getValue();
        assertThat(passed.getAccessToken()).isEqualTo("A");
        assertThat(passed.getRefreshToken()).isEqualTo("R");
    }

    @Test
    @DisplayName("login: actualLogin 과정에서 MemberException -> loginFailed 호출 후 예외 발생")
    void login_failure_callsLoginFailed() throws Exception {
        AuthLoginRequest request = new AuthLoginRequest("alice","1234");
        when(actualLoginManager.validateMember(request))
            .thenThrow(new MemberException(ErrorCode.GLOBAL_ERROR));

        assertThatThrownBy(() -> service.login(request, req))
            .isInstanceOf(MemberException.class);

        verify(bruteForceManager).preCheck("alice", req);
        verify(bruteForceManager).loginFailed("alice");
        verify(bruteForceManager, never()).loginSucceeded(anyString());
    }

    @Test
    @DisplayName("actualLogin: 매니저 3단계 호출 후 TokenResponse 반환 (인스턴스 동일성에 의존하지 않음)")
    void actualLogin_success() {
        AuthLoginRequest request = new AuthLoginRequest("alice","pw");
        Member m = Member.builder()
            .id("m1")
            .name("alice")
            .nickName("Ali")
            .build();

        when(actualLoginManager.validateMember(request)).thenReturn(m);
        when(actualLoginManager.generateToken(m)).thenReturn(new JwtTokenResponse("A","R"));

        // void any() 스텁
        doNothing().when(actualLoginManager).updateMemberRefreshToken(eq(m), any(JwtTokenResponse.class));

        TokenResponse res = service.actualLogin(request);

        assertThat(res.getAccessToken()).isEqualTo("A");
        assertThat(res.getRefreshToken()).isEqualTo("R");
        verify(actualLoginManager).updateMemberRefreshToken(eq(m), any(JwtTokenResponse.class));
    }

    @Test
    @DisplayName("refreshAccessToken: 검증 통과 -> 새 토큰 발급, 세션/DB RT 업데이트")
    void refreshAccessToken_success() {
        String rt = "refresh.jwt";
        when(jwtProvider.validateToken(rt)).thenReturn(true);
        when(jwtProvider.isTokenExpired(rt)).thenReturn(false);

        Member member = Member.builder()
            .id("m1")
            .name("alice")
            .nickName("Ali")
            .build();

        Auth auth = Auth.builder()
            .id("auth-1")
            .member(member)
            .refreshToken(rt)
            .build();

        when(authRepository.findByRefreshToken(rt)).thenReturn(Optional.of(auth));
        when(jwtProvider.generateAccessToken("m1","alice","Ali")).thenReturn("new.access");
        when(jwtProvider.generateRefreshToken("m1")).thenReturn("new.refresh");

        JwtTokenResponse res = service.refreshAccessToken(rt);

        assertThat(res.getAccessToken()).isEqualTo("new.access");
        assertThat(res.getRefreshToken()).isEqualTo("new.refresh");

        verify(jwtRedisService).saveRefreshTokenToSessionRedis("auth-1", "new.refresh");
        assertThat(auth.getRefreshToken()).isEqualTo("new.refresh");
    }

    @Test
    @DisplayName("refreshAccessToken: validate 실패 -> INVALID_JWT_TOKEN")
    void refreshAccessToken_invalid() {
        when(jwtProvider.validateToken("bad")).thenReturn(false);
        assertThatThrownBy(() -> service.refreshAccessToken("bad"))
            .isInstanceOf(MemberException.class);
        verifyNoInteractions(authRepository);
    }

    @Test
    @DisplayName("logout: access 블랙리스트 업로드 + RT 제거(Valkey/RDB)")
    void logout_success() {
        String authz = "Bearer access.jwt";
        String refresh = "refresh.jwt";

        when(authRedisManager.calculateRemainingExpiration("access.jwt")).thenReturn(120000L);

        Member member = Member.builder()
            .id("m1")
            .build();

        Auth auth = Auth.builder()
            .id("auth-1")
            .member(member)
            .refreshToken(refresh)
            .build();

        when(authRepository.findByRefreshToken(refresh)).thenReturn(Optional.of(auth));

        service.logout(authz, refresh);

        verify(authRedisManager).uploadAccessTokenToRedis("blacklist:access_token:access.jwt", 120000L);
        verify(authRedisManager).deleteRefreshToken(refresh);
        assertThat(auth.getRefreshToken()).isNull();
    }



}
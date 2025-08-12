package org.com.dungeontalk.domain.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.auth.dto.request.AuthLoginRequest;
import org.com.dungeontalk.domain.auth.dto.request.RefreshTokenRequest;
import org.com.dungeontalk.domain.auth.dto.response.AuthLoginResponse;
import org.com.dungeontalk.domain.auth.dto.response.JwtTokenResponse;
import org.com.dungeontalk.domain.auth.service.AuthService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 로그인
    @PostMapping("/login")
    public RsData<AuthLoginResponse> login(
            @RequestBody AuthLoginRequest request,
            HttpServletResponse httpServletResponse) {

        // 로그인 서비스 레이어 호출
        AuthLoginResponse jwtTokenResponse = authService.login(request);

        // 쿠키에 리프레시 토큰 저장
        authService.saveRefreshTokenToCookie(httpServletResponse, jwtTokenResponse.refreshToken());

        return RsData.of("200", "로그인 성공", jwtTokenResponse);
    }

    // JWT 토큰 재발급
    @PostMapping("/refresh")
    public RsData<JwtTokenResponse> refreshToken(
            @RequestBody RefreshTokenRequest request,
            HttpServletResponse httpServletResponse) {

        // RTR 서비스 레이어 호출
        JwtTokenResponse jwtTokenResponse = authService.refreshAccessToken(request.getRefreshToken());

        // 쿠키에 리프레시 토큰 저장
        authService.saveRefreshTokenToCookie(httpServletResponse, jwtTokenResponse.getRefreshToken());

        return RsData.of("200", "토큰 재발급 성공", jwtTokenResponse);
    }

    //  로그 아웃
    @PostMapping("/logout")
    public RsData<String> logout(
            @RequestHeader("Authorization") String authorizationHeader,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse httpServletResponse
    ) {

        // 로그아웃 서비스 레이어 호출
        authService.logout(authorizationHeader, refreshToken);

        // 쿠키에서 리프레시 토큰 제거
        authService.removeRefreshTokenCookie(httpServletResponse);

        return RsData.of("200", "로그아웃 완료", null);
    }

}

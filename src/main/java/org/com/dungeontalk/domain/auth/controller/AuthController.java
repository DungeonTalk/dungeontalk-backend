package org.com.dungeontalk.domain.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.auth.dto.request.AuthLoginRequest;
import org.com.dungeontalk.domain.auth.dto.request.RefreshTokenRequest;
import org.com.dungeontalk.domain.auth.dto.response.AuthLoginResponse;
import org.com.dungeontalk.domain.auth.dto.response.JwtTokenResponse;
import org.com.dungeontalk.domain.auth.dto.response.RefreshTokenResponse;
import org.com.dungeontalk.domain.auth.service.AuthService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 로그인
    @PostMapping("/login")
    public RsData<AuthLoginResponse> login(@RequestBody AuthLoginRequest request) {
        AuthLoginResponse response = authService.login(request);
        return RsData.of("200", "로그인 성공", response);
    }

    // JWT 토큰 재발급
    @PostMapping("/refresh")
    public RsData<JwtTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("컨트롤러 진입");

        JwtTokenResponse jwtTokenResponse = authService.refreshAccessToken(request.getRefreshToken());
        return RsData.of("200", "토큰 재발급 성공", jwtTokenResponse);
    }

    // 로그 아웃
    @PostMapping("/logout")
    public RsData<String> logout(HttpServletRequest request) {
        authService.logout(request);
        return RsData.of("200", "로그아웃 완료", null);
    }
}

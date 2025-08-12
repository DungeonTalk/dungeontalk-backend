package org.com.dungeontalk.domain.auth.controller;

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
    public RsData<AuthLoginResponse> login(@RequestBody AuthLoginRequest request) {
        AuthLoginResponse response = authService.login(request);
        return RsData.of("200", "로그인 성공", response);
    }

    // JWT 토큰 재발급
    @PostMapping("/refresh")
    public RsData<JwtTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {

        JwtTokenResponse jwtTokenResponse = authService.refreshAccessToken(request.getRefreshToken());
        return RsData.of("200", "토큰 재발급 성공", jwtTokenResponse);
    }

    //  로그 아웃
    @PostMapping("/logout")
    public RsData<String> logout(
            @RequestHeader("Authorization") String authorizationHeader,
            @CookieValue(value = "refreshToken", required = false) String refreshToken
    ) {
        authService.logout(authorizationHeader, refreshToken);
        return RsData.of("200", "로그아웃 완료", "good");
    }

}

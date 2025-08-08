package org.com.dungeontalk.domain.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
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

    // TODO : 여기 하고 있었음 !!!
    // JWT 토큰 재발급
    @PostMapping("/token/refresh")
    public RsData<JwtTokenResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        JwtTokenResponse jwtTokenResponse = authService.refreshAccessToken(request.getRefreshToken());
        if (jwtTokenResponse == null) {
            return RsData.of("401", "리프레시 토큰이 유효하지 않습니다.", null);
        }
        //RefreshTokenResponse response = new RefreshTokenResponse(newAccessToken);
        return RsData.of("200", "토큰 재발급 성공", null);
    }


    // 로그 아웃
    @PostMapping("/logout")
    public RsData<String> logout(HttpServletRequest request) {
        authService.logout(request);
        return RsData.of("200", "로그아웃 완료", null);
    }
}

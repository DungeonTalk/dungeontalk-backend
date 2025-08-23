package org.com.dungeontalk.domain.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.auth.dto.request.AuthLoginRequest;
import org.com.dungeontalk.domain.auth.dto.request.RefreshTokenRequest;
import org.com.dungeontalk.domain.auth.dto.response.AuthLoginResponse;
import org.com.dungeontalk.domain.auth.dto.response.JwtTokenResponse;
import org.com.dungeontalk.domain.auth.dto.response.TokenResponse;
import org.com.dungeontalk.domain.auth.manager.CookieManager;
import org.com.dungeontalk.domain.auth.service.AuthService;
import org.com.dungeontalk.global.rsData.RsData;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final CookieManager cookieManager;

    // API 로그인 (JSON)
    @PostMapping("/login")
    @ResponseBody
    public RsData<AuthLoginResponse> login(
            @RequestBody AuthLoginRequest request,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws InterruptedException {

        // 로그인 서비스 레이어 호출
        TokenResponse tokenResponse = authService.login(request,httpServletRequest);

        // 쿠키에 리프레시 토큰 저장
        authService.saveRefreshTokenToCookie(httpServletResponse, tokenResponse.getRefreshToken());

        return RsData.of("200", "로그인 성공", new AuthLoginResponse(tokenResponse.getAccessToken()));
    }
    
    // 서버사이드 로그인 (폼 제출)
    @PostMapping("/server-login")
    public String serverLogin(
            @ModelAttribute AuthLoginRequest loginRequest,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        
        try {
            // 로그인 처리
            TokenResponse tokenResponse = authService.login(loginRequest, request);
            
            // 토큰을 쿠키에 저장
            cookieManager.addAccessTokenCookie(response, tokenResponse.getAccessToken());
            cookieManager.addRefreshTokenCookie(response, tokenResponse.getRefreshToken());
            
            // 사용자 정보를 쿠키에 저장 (JavaScript에서 읽기 가능)
//            cookieManager.addUserInfoCookie(response, loginRequest.name(), loginResponse.memberId());
            
            log.info("서버사이드 로그인 성공: {}", loginRequest.name());
            
            // 게임 페이지로 리다이렉트
            return "redirect:/game";
            
        } catch (Exception e) {
            log.error("서버사이드 로그인 실패: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.");
            return "redirect:/login";
        }
    }

    // JWT 토큰 재발급
    @PostMapping("/refresh")
    @ResponseBody
    public RsData<AuthLoginResponse> refreshToken(
            @RequestBody RefreshTokenRequest request,
            HttpServletResponse httpServletResponse) {

        // RTR 서비스 레이어 호출
        JwtTokenResponse jwtTokenResponse = authService.refreshAccessToken(request.getRefreshToken());

        // 쿠키에 리프레시 토큰 저장
        authService.saveRefreshTokenToCookie(httpServletResponse, jwtTokenResponse.getRefreshToken());

        return RsData.of("200", "토큰 재발급 성공", new AuthLoginResponse(jwtTokenResponse.getAccessToken()));
    }

    // API 로그아웃 (JSON)
    @PostMapping("/logout")
    @ResponseBody
    public RsData<String> logout(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @CookieValue(value = "accessToken", required = false) String accessToken,
            HttpServletResponse response) {
        
        // accessToken 쿠키가 있으면 Authorization 헤더로 변환
        if (authorizationHeader == null && accessToken != null) {
            authorizationHeader = "Bearer " + accessToken;
        }
        
        // 로그아웃 처리
        if (authorizationHeader != null || refreshToken != null) {
            authService.logout(authorizationHeader, refreshToken);
        }
        
        // 모든 인증 관련 쿠키 일괄 제거
        cookieManager.clearAllAuthCookies(response);
        
        return RsData.of("200", "로그아웃 완료", null);
    }
    
    // 서버사이드 로그아웃 (폼 제출)
    @PostMapping("/server-logout")
    public String serverLogout(
            HttpServletRequest request,
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            @CookieValue(value = "accessToken", required = false) String accessToken,
            HttpServletResponse response) {
        
        try {
            String authorizationHeader = null;
            
            // 쿠키에서 accessToken 추출
            if (accessToken != null) {
                authorizationHeader = "Bearer " + accessToken;
            }
            
            // 로그아웃 처리
            if (authorizationHeader != null || refreshToken != null) {
                authService.logout(authorizationHeader, refreshToken);
                log.info("서버사이드 로그아웃 성공");
            }
            
            // SecurityContext 클리어 (중요!)
            SecurityContextHolder.clearContext();
            
            // 세션 무효화
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            
            // CookieManager를 통해 모든 쿠키 제거
            cookieManager.clearAllAuthCookies(response);
            
        } catch (Exception e) {
            log.error("서버사이드 로그아웃 중 오류 발생", e);
        }
        
        // 로그인 페이지로 리다이렉트
        return "redirect:/login";
    }

}

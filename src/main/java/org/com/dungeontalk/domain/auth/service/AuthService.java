package org.com.dungeontalk.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.auth.dto.request.AuthLoginRequest;
import org.com.dungeontalk.domain.auth.dto.response.AuthLoginResponse;
import org.com.dungeontalk.domain.auth.dto.response.JwtTokenResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final AuthRepository authRepository;
    private final JwtProvider jwtProvider;
    private final JwtRedisService jwtRedisService;
    private final AuthRedisManager authRedisManager;
    private final CookieManager cookieManager;
    private final BruteForceManager bruteForceManager;
    private final ActualLoginManager actualLoginManager;

    // 보안 기능이 추가 된 로그인 메서드
    public AuthLoginResponse login(AuthLoginRequest request, HttpServletRequest httpServletRequest) throws InterruptedException {

        bruteForceManager.preCheck(request.name(), httpServletRequest); // 로그인 시도 전 이상 행동 존재 유무 체크
        try {
            AuthLoginResponse authLoginResponse = actualLogin(request); // 실질적인 로그인 메서드 호출
            bruteForceManager.loginSucceeded(request.name()); // 로그인 성공시 기존 실패/정지 기록 삭제
            return authLoginResponse;
        } catch (MemberException ex) {
            bruteForceManager.loginFailed(request.name()); // Delay, Cool Down 방어
            throw ex;
        }
    }

    // 실질적인 로그인 메서드
    public AuthLoginResponse actualLogin(AuthLoginRequest request) {

        Member member = actualLoginManager.validateMember(request); // 유저 검증
        JwtTokenResponse jwtTokenResponse = actualLoginManager.generateToken(member); // JWT 토큰 생성
        actualLoginManager.updateMemberRefreshToken(member, jwtTokenResponse); // RefreshToken 갱신

        return new AuthLoginResponse(member.getId(),
                jwtTokenResponse.getAccessToken(),
                jwtTokenResponse.getRefreshToken()
        );
    }

    // 리프레시 토큰을 통한 새로운 JWT 토큰 생성
    public JwtTokenResponse refreshAccessToken(String refreshToken) {

        // REFACTOR GUIDE
        /* 유효성 검사 */
        /* 새로운 JWT 생성 */
        /* 새로운 JWT 적용 */

        // 토큰 서명/포맷 검사
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new MemberException(ErrorCode.INVALID_JWT_TOKEN);
        }

        // 토큰 만료 검사
        if (jwtProvider.isTokenExpired(refreshToken)) {
            throw new MemberException(ErrorCode.EXPIRED_JWT_TOKEN);
        }

        // 사용자 조회
        Auth auth = authRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new MemberException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 새로운 Access Token과 Refresh Token을 반환
        String newAccessToken = jwtProvider.generateAccessToken(
                auth.getMember().getId(),
                auth.getMember().getName(),
                auth.getMember().getNickName()
        );
        String newRefreshToken = jwtProvider.generateRefreshToken(auth.getMember().getId());

        // Session에 RT 최신화
        jwtRedisService.saveRefreshTokenToSessionRedis(auth.getId(), newRefreshToken);

        // RDB에 RT 최신화
        auth.setRefreshToken(newRefreshToken);

        // 클라이언트에게 JWT 전달
        return new JwtTokenResponse(newAccessToken, newRefreshToken);
    }

    // 로그 아웃 메서드
    public void logout(String authorizationHeader, String refreshToken){

        // 엑세스 토큰 추출
        String accessToken = null;
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            accessToken = authorizationHeader.substring(7);
        }

        // 엑세스 토큰 블랙리스트 추가
        String accessKey = "blacklist:access_token:" + accessToken;
        long remainExpiration = authRedisManager.calculateRemainingExpiration(accessToken);
        authRedisManager.uploadAccessTokenToRedis(accessKey, remainExpiration);

        //  RDB에서 RT 제거
        Auth auth = authRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new MemberException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
        auth.setRefreshToken(null);

        // Valkey에서 RT 제거
        authRedisManager.deleteRefreshToken(refreshToken);

    }

    // 리프레시 토큰을 쿠키에 세팅
    public void saveRefreshTokenToCookie(HttpServletResponse response, String refreshToken) {
        cookieManager.addRefreshTokenCookie(response, refreshToken);
    }

    // 리프레시 토큰을 쿠키에서 제거
    public void removeRefreshTokenCookie(HttpServletResponse response) {
        cookieManager.clearRefreshTokenCookie(response);
    }

}
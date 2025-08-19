package org.com.dungeontalk.domain.auth.manager;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.auth.entity.Auth;
import org.com.dungeontalk.domain.auth.repository.AuthRepository;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.MemberException;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogoutManager {


    private final AuthRepository authRepository;
    private final AuthRedisManager authRedisManager;

    // Access Token 추출
    public String parseAccessToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    // 로그아웃 한 유저의 엑세스 토큰을 Redis에 블랙리스트 처리
    public void blacklistAccessToken(String accessToken) {
        if (accessToken != null) {
            String accessKey = "blacklist:access_token:" + accessToken;
            long remainExpiration = authRedisManager.calculateRemainingExpiration(accessToken);
            authRedisManager.uploadAccessTokenToRedis(accessKey, remainExpiration);
        }
    }

    // DB에서 로그아웃 한 유저의 리프레시 토큰 삭제
    public void removeRefreshTokenFromDB(String refreshToken) {
        Auth auth = authRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new MemberException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
        auth.setRefreshToken(null);
    }

    // Redis에서 로그아웃 한 유저의 리프레시 토큰 삭제
    public void removeRefreshTokenFromRedis(String refreshToken) {
        authRedisManager.deleteRefreshToken(refreshToken);
    }
}

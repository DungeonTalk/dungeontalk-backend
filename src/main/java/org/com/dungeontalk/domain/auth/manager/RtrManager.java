package org.com.dungeontalk.domain.auth.manager;


import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.domain.auth.entity.Auth;
import org.com.dungeontalk.domain.auth.repository.AuthRepository;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.MemberException;
import org.com.dungeontalk.global.security.JwtProvider;
import org.com.dungeontalk.global.security.JwtRedisService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RtrManager {

    private final JwtProvider jwtProvider;
    private final AuthRepository authRepository;
    private final JwtRedisService jwtRedisService;

    // 토큰 유효성 검사
    public void validateRefreshToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new MemberException(ErrorCode.INVALID_JWT_TOKEN);
        }
        if (jwtProvider.isTokenExpired(refreshToken)) {
            throw new MemberException(ErrorCode.EXPIRED_JWT_TOKEN);
        }
    }

    // RefreshToken으로 Auth 조회
    public Auth findAuthByRefreshToken(String refreshToken) {
        return authRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new MemberException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
    }

    // Access Token 생성
    public String generateNewAccessToken(Auth auth) {
        return jwtProvider.generateAccessToken(
                auth.getMember().getId(),
                auth.getMember().getName(),
                auth.getMember().getNickName()
        );
    }

    // Refresh Token 생성
    public String generateNewRefreshToken(Auth auth) {
        return jwtProvider.generateRefreshToken(auth.getMember().getId());
    }

    // 새 Refresh Token 적용 (Redis + DB)
    public void applyNewRefreshToken(Auth auth, String newRefreshToken) {
        jwtRedisService.saveRefreshTokenToSessionRedis(auth.getId(), newRefreshToken);
        auth.setRefreshToken(newRefreshToken);
    }
}
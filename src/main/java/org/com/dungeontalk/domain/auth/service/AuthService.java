package org.com.dungeontalk.domain.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.auth.dto.request.AuthLoginRequest;
import org.com.dungeontalk.domain.auth.dto.response.AuthLoginResponse;
import org.com.dungeontalk.domain.auth.dto.response.JwtTokenResponse;
import org.com.dungeontalk.domain.auth.entity.Auth;
import org.com.dungeontalk.domain.auth.manager.AuthRedisManager;
import org.com.dungeontalk.domain.auth.repository.AuthRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.MemberException;
import org.com.dungeontalk.global.security.JwtProvider;
import org.com.dungeontalk.global.security.JwtRedisService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberRepository memberRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtRedisService jwtRedisService;
    private final AuthRedisManager authRedisManager;

    // 로그인 메서드
    public AuthLoginResponse login(AuthLoginRequest request) {

        // 회원 조회
        Member member = memberRepository.findByName(request.name())
                .orElseThrow(() -> new MemberException(ErrorCode.GLOBAL_ERROR));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new MemberException(ErrorCode.GLOBAL_ERROR);
        }

        // 토큰 생성
        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getName(), member.getNickName());
        String refreshToken = jwtProvider.generateRefreshToken(member.getId());

       // Auth 엔티티 생성
        Optional<Auth> existingAuthOpt = authRepository.findByMember(member);

        if (existingAuthOpt.isPresent()) {
            Auth auth = existingAuthOpt.get();
            auth.setAccessToken(null);
            auth.setRefreshToken(refreshToken);
            authRepository.save(auth);
        } else {
            Auth newAuth = Auth.builder()
                    .member(member)
                    .email(member.getName())
                    .tokenType("bearer")
                    .accessToken(null)
                    .refreshToken(refreshToken)
                    .build();

            authRepository.save(newAuth);

        }

        // session에 저장
        jwtRedisService.saveRefreshTokenToSessionRedis(member.getId(), refreshToken);

        return new AuthLoginResponse(
                member.getId(),
                accessToken,
                refreshToken
        );
    }

    // 리프레시 토큰을 통한 새로운 JWT 토큰 생성
    public JwtTokenResponse refreshAccessToken(String refreshToken) {

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
//        String newAccessToken = jwtProvider.generateAccessToken(auth.getId(), auth.getMember().getName(), auth.getMember().getNickName());
//        String newRefreshToken = jwtProvider.generateRefreshToken(auth.getId());
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

        /* 로그인 전 엑세스 토큰을 불러오는지 확인 -> 아님 현재의 엑세스 토큰을 넣어도 현재 엑세스 토큰을 가져옴 */
        System.out.println("Access token : " + accessToken);

        // 엑세스 토큰 블랙리스트 추가
        String accessKey = "blacklist:access_token:" + accessToken;
        long remainExpiration = authRedisManager.calculateRemainingExpiration(accessToken);
        authRedisManager.uploadAccessTokenToRedis(accessKey, remainExpiration);

        //  RDB에서 해시 된 RT 제거
        Auth auth = authRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new MemberException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
        auth.setRefreshToken(null);

        // Valkey에서 해시 된 RT 제거
        authRedisManager.deleteRefreshToken(refreshToken);

    }


}
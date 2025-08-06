package org.com.dungeontalk.domain.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.auth.dto.request.AuthLoginRequest;
import org.com.dungeontalk.domain.auth.dto.response.AuthLoginResponse;
import org.com.dungeontalk.domain.auth.entity.Auth;
import org.com.dungeontalk.domain.auth.entity.AuthId;
import org.com.dungeontalk.domain.auth.repository.AuthRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.MemberException;
import org.com.dungeontalk.global.security.JwtService;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final String PROVIDER_LOCAL = "LOCAL";

    private final MemberRepository memberRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    // 로그인 메서드
    public AuthLoginResponse login(AuthLoginRequest request) {

        // 1. 회원 조회 (name 기준)
        Member member = memberRepository.findByName(request.name())
                .orElseThrow(() -> new MemberException(ErrorCode.GLOBAL_ERROR));

        // 2. 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new MemberException(ErrorCode.GLOBAL_ERROR);
        }

        // 3. 토큰 생성
        String accessToken = jwtService.generateAccessToken(member.getId(), member.getName(), member.getNickName());
        String refreshToken = jwtService.generateRefreshToken(member.getId(), member.getName(), member.getNickName());
        log.info("엑세스 토큰 생성 : {}",accessToken);
        log.info("리프레시 토큰 생성 : {}",refreshToken);

        // 4. Auth 엔티티 갱신 또는 생성 (AuthId 사용하지 않음)
        Optional<Auth> existingAuthOpt = authRepository.findByMember(member);
        log.info("existingAuthOpt : {}", existingAuthOpt);

        if (existingAuthOpt.isPresent()) {
            log.info("1");
            Auth auth = existingAuthOpt.get();
            auth.setAccessToken(accessToken);
            auth.setRefreshToken(refreshToken);
            authRepository.save(auth);
            log.info("2");
        } else {
            log.info("3");
            Auth newAuth = Auth.builder()
                    .member(member)
                    .email(member.getName())
                    .tokenType("bearer")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
            log.info("newAuth: {}", newAuth);
            log.info("4");
            authRepository.save(newAuth);
            log.info("5");
        }

        // RefreshToken을 세션 레디스에 저장
        // todo : Accesstoken또 추가하기
        // jwtService.saveRefreshTokenToSessionRedis(member.getId(), refreshToken);

        log.info("7");
        return new AuthLoginResponse(
                member.getId(),
                accessToken,
                refreshToken
        );
    }

    // 로그 아웃 메서드
    public void logout(HttpServletRequest request) {
        String accessToken = jwtService.extractAccessToken(request);
        if (accessToken == null) {
            log.warn("logout called but access token is missing");
            SecurityContextHolder.clearContext();
            return;
        }

        // 2) 토큰에서 memberId 추출
        String memberId;
        try {
            memberId = jwtService.extractIdFromToken(accessToken);
        } catch (Exception ex) {
            log.warn("failed to extract member id from token during logout: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
            return;
        }

        // 3) Auth 레코드 찾아 토큰 제거
//        AuthId authId = new AuthId(PROVIDER_LOCAL, memberId);
//        Optional<Auth> authOpt = authRepository.findById(authId);

        // Auth 레코드 찾아 토큰 제거 (memberId로 직접 조회)
        Optional<Auth> authOpt = authRepository.findByMember_Id(memberId);
        if (authOpt.isPresent()) {
            Auth auth = authOpt.get();
            auth.setAccessToken(null);
            auth.setRefreshToken(null);
            authRepository.save(auth);
            log.info("cleared auth tokens for memberId={}", memberId);
        } else {
            log.info("no auth record found for memberId={} during logout", memberId);
        }

        // (권장) JwtService에 레디스에서 refresh token 삭제하는 메서드가 있다면 호출하세요.
        // ex) jwtService.deleteRefreshTokenFromSessionRedis(memberId);

        // 4) Security context clear
        SecurityContextHolder.clearContext();
    }
}
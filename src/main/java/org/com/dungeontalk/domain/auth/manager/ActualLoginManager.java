package org.com.dungeontalk.domain.auth.manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.com.dungeontalk.domain.auth.dto.request.AuthLoginRequest;
import org.com.dungeontalk.domain.auth.dto.response.JwtTokenResponse;
import org.com.dungeontalk.domain.auth.entity.Auth;
import org.com.dungeontalk.domain.auth.repository.AuthRepository;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.MemberException;
import org.com.dungeontalk.global.security.JwtProvider;
import org.com.dungeontalk.global.security.JwtRedisService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActualLoginManager {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final AuthRepository authRepository;
    private final JwtRedisService jwtRedisService;

    /**
     * 로그인 시 회원 조회 및 비밀번호 검증
     * @param request - 유저의 로그인 요청 데이터
     * @return - 인증 받은 멤버 객체
     */
    public Member validateMember(AuthLoginRequest request){

        // 회원 조회
        Member member = memberRepository.findByName(request.name())
                .orElseThrow(() -> new MemberException(ErrorCode.GLOBAL_ERROR));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new MemberException(ErrorCode.GLOBAL_ERROR);
        }

        return member;
    }

    /**
     * Jwt 토큰 생성 메서드
     * @param member 인증된 멤버 객체
     * @return JWT 토큰 DTO
     */
    public JwtTokenResponse generateToken(Member member) {

        String accessToken = jwtProvider.generateAccessToken(member.getId(), member.getName(), member.getNickName());
        String refreshToken = jwtProvider.generateRefreshToken(member.getId());

        return new JwtTokenResponse(accessToken, refreshToken);
    }

    /**
     * RefreshToken 갱신
     * @param member 인증된 유저
     * @param jwtTokenResponse JWT 토큰 DTO
     */
    public void updateMemberRefreshToken(Member member, JwtTokenResponse jwtTokenResponse) {

        // Auth 엔티티 생성
        Optional<Auth> existingAuthOpt = authRepository.findByMember(member);

        if (existingAuthOpt.isPresent()) {
            Auth auth = existingAuthOpt.get();
            auth.setAccessToken(null);
            auth.setRefreshToken(jwtTokenResponse.getRefreshToken());
            authRepository.save(auth);
        } else {
            Auth newAuth = Auth.builder()
                    .member(member)
                    .email(member.getName())
                    .tokenType("bearer")
                    .accessToken(null)
                    .refreshToken(jwtTokenResponse.getRefreshToken())
                    .build();

            authRepository.save(newAuth);
        }

        // session에 저장
        jwtRedisService.saveRefreshTokenToSessionRedis(member.getId(), jwtTokenResponse.getRefreshToken());

    }




}

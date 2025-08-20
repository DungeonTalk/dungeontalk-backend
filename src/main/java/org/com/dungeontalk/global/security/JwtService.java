package org.com.dungeontalk.global.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.com.dungeontalk.domain.member.entity.Member;
import org.com.dungeontalk.domain.member.repository.MemberRepository;
import org.com.dungeontalk.global.exception.ErrorCode;
import org.com.dungeontalk.global.exception.customException.MemberException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    // 토큰에서 멤버 객체 생성
    public Member getMemberFromToken(String token) {

        String memberId = extractIdFromToken(token);
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.GLOBAL_ERROR));
    }

    // 토큰에서 고유 번호 추출
    public String extractIdFromToken(String token) {

        return jwtProvider.extractClaims(token).get("id", String.class);
    }

    public CustomUserDetails getUserDetailsFromToken(String accessToken) {
        Claims claims = jwtProvider.extractClaims(accessToken);

        String id = claims.get("id", String.class);
        String name = claims.get("name", String.class);
        String nickName = claims.get("nickName", String.class);

        // ======================= BEFORE VERSION =========================
        /* 혹은 DB를 거치는 방법 : 캐싱 필요 - 매 요청마다 DB를 거치면 부담이 크기 때문 */
        // 토큰 유효성 검사 및 멤버 조회 // 캐싱의 대상
        //Member member = jwtService.getMemberFromToken(accessToken);
        // 인증 정보 생성 및 SecurityContext에 저장
        //CustomUserDetails userDetails = new CustomUserDetails(member);
        // ======================= BEFORE VERSION =========================

        return new CustomUserDetails(id, name, nickName);
    }



}

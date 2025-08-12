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


}

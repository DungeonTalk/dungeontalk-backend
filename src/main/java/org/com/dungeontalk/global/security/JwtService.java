package org.com.dungeontalk.global.security;


import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.servlet.http.HttpServletRequest;
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

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class JwtService {

    private final RedisTemplate<String, String> cacheRedis;
    private final RedisTemplate<String, String> sessionRedis;
    private final MemberRepository memberRepository;

    public JwtService(
            @Qualifier("cacheRedisTemplate") RedisTemplate<String, String> cacheRedis,
            @Qualifier("sessionRedisTemplate") RedisTemplate<String, String> sessionRedis,
            MemberRepository memberRepository) {
        this.cacheRedis = cacheRedis;
        this.sessionRedis = sessionRedis;
        this.memberRepository = memberRepository;
    }

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.accessexpiration}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;

    @Value("${jwt.refreshexpiration}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;

    // 엑세스 토큰 생성
    public String generateAccessToken(String id, String name,String nickName) {
        return Jwts.builder()
                .setSubject("dgt-User")
                .claim("id", id)
                .claim("name", name)
                .claim("nickName", nickName)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // 리프레시 토큰 생성
    public String generateRefreshToken(String id, String name,String nickName) {
        return Jwts.builder()
                .setSubject("dgt-User")
                .claim("id", id)
                .claim("name", name)
                .claim("nickName", nickName)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS256, SECRET_KEY)
                .compact();
    }

    // 토큰에서 멤버 객체 생성
    public Member getMemberFromToken(String token) {
        String memberId = extractIdFromToken(token);
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.GLOBAL_ERROR));
    }

    // 유저 요청으로 부터 엑세스 토큰 추출
    public String extractAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    // 토큰에서 고유 번호 추출
    public String extractIdFromToken(String token) {

        return extractClaims(token).get("id", String.class);
    }

    // 리프레쉬 토큰 세션 레디스에 저장 메서드
    public void saveRefreshTokenToSessionRedis(String memberId,String refreshToken) {

        String key = "refresh_token:" + memberId;
        sessionRedis.opsForValue().set(key, refreshToken);
        //sessionRedis.opsForValue().set(key, refreshToken, REFRESH_TOKEN_EXPIRATION_TIME); -> 문제의 원인, REFRESH_TOKEN_EXPIRATION_TIME

    }

    // 토큰에서 클레임 추출
    public Claims extractClaims(String token) {
        return Jwts.parser() // JWT 파서 객체 생성
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {

        try {
            Claims claims = extractClaims(token);
            return !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            log.error("Token 유효성 검증 실패 : {}", token, e);
            return false;
        }
    }

    public boolean isTokenBlacklisted(String token) {
        String hashedKey = DigestUtils.sha256Hex(token);
        return Boolean.TRUE.equals(sessionRedis.hasKey("blacklist:" + hashedKey));
    }

}

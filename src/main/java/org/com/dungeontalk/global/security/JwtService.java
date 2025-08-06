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

    // ======================= 토큰 생성 로직 =========================


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

    // ======================= 토큰에서 멤버 객체 추출 로직 =========================

    public Member getMemberFromToken(String token) {
        String memberId = extractIdFromToken(token);
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(ErrorCode.GLOBAL_ERROR));
    }

    // ======================= 요청에서 엑시스 토큰 추출 로직 =========================

    /**
     * 사용자 요청으로 부터 엑세스 토큰 추출
     *
     * @param request 사용자 요청
     * @return 엑세스 토큰 반환
     */
    public String extractAccessToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return null;
    }

    // ======================= 요청에서 유저 고유 번호 추출 로직 =========================

    public String extractIdFromRequest(HttpServletRequest request) {
        String accessToken = extractAccessToken(request);
        return extractIdFromToken(accessToken);
    }

    public String extractIdFromToken(String token) {
        return extractClaims(token).get("id", String.class);
    }

    // ======================= 요청에서 유저 nickName 추출 로직 =========================

    /**
     * [1단계] HTTP 요청에서 액세스 토큰을 추출하고, JWT 내부의 nickName 클레임을 반환한다.
     *
     * @param request HTTP 요청 객체
     * @return JWT 토큰에서 추출한 유저 nickName (String)
     */
    public String extractNickNameFromRequest(HttpServletRequest request) {
        String accessToken = extractAccessToken(request);
        return extractNickNameFromToken(accessToken);
    }

    /**
     * [2단계] JWT 토큰에서 nickName 클레임 값을 String 타입으로 파싱한다.
     *
     * @param token JWT 액세스 토큰
     * @return nickName 클레임 값 (String)
     *
     * @see #extractClaims(String) 클레임 추출 메서드
     */
    public String extractNickNameFromToken(String token) {
        return extractClaims(token).get("nickName", String.class);
    }

    // ======================= RefreshToken 저장 및 refresh token rotation 로직  =========================

    /**
     * 리프레쉬 토큰 세션 레디스에 저장 메서드
     *
     * @param memberId 로그인하는 유저의 고유 번호
     * @param refreshToken 로그인 한 후 반환된 리프레쉬 토큰(세션에 저장)
     */
    public void saveRefreshTokenToSessionRedis(Integer memberId,String refreshToken) {

        log.info("length : {}",refreshToken.getBytes().length);
        String key = "refresh_token:" + memberId;
        sessionRedis.opsForValue().set(key, refreshToken, REFRESH_TOKEN_EXPIRATION_TIME);

    }

    // ======================= Util Code =========================

    public Claims extractClaims(String token) {
        return Jwts.parser() // JWT 파서 객체 생성
                .setSigningKey(SECRET_KEY)
                .parseClaimsJws(token)
                .getBody();
    }

}

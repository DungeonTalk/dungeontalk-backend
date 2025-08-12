package org.com.dungeontalk.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Slf4j
@Service
public class JwtProvider {
    @Value("${jwt.secret}")
    private String SECRET_KEY;

    @Value("${jwt.accessexpiration}")
    private long ACCESS_TOKEN_EXPIRATION_TIME;

    @Value("${jwt.refreshexpiration}")
    private long REFRESH_TOKEN_EXPIRATION_TIME;

    // 엑세스 토큰 생성 v2
    public String generateAccessToken(String id, String name, String nickName) {
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(id) // 보편적으로 PK가 subject
                .claim("id",id)
                .claim("name", name)
                .claim("nickName", nickName)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256) // 비대칭 키 적용
                .compact();
    }

    // 리프레시 토큰 생성 v2
    public String generateRefreshToken(String id) {
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .setSubject(id) // PK를 subject로
                .claim("type", "refresh") // 토큰 타입 명시
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰에서 클레임 추출 ver 2.0
    public Claims extractClaims(String token) {

        System.out.println("토큰에서 클레임 추출 메서드 진입 ");
        Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));

        System.out.println("토큰에서 클레임 추출 메서드 key 추출 ");
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // 토큰 유효성 검사 ver 2.0
    public boolean validateToken(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token); // 서명, 포맷, 만료 검증
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // 토큰 만료 여부 검사 ver 2.0
    public boolean isTokenExpired(String token) {
        try {
            Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes(StandardCharsets.UTF_8));
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Date expiration = claims.getExpiration();
            return expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;  // 파싱 실패도 만료로 처리
        }
    }

    // ======================= JWT Version Issue =========================

    // 토큰에서 클레임 추출
//    public Claims extractClaims(String token) {
//        return Jwts.parser() // JWT 파서 객체 생성
//                .setSigningKey(SECRET_KEY)
//                .parseClaimsJws(token)
//                .getBody();
//    }


    // JWT 토큰 검증 - 유효성 검사
    /* 에러 발견 : AuthService의 refresAccessToken에서 발생 */
//    public boolean validateToken(String token) {
//        try {
//            Jwts.parserBuilder()
//                    .setSigningKey(SECRET_KEY)
//                    .build()
//                    .parseClaimsJws(token); // 토큰 파싱 및 서명, 포맷, 만료 검증 수행
//            return true;
//        } catch (JwtException | IllegalArgumentException e) {
//            // 서명 불일치, 토큰 만료, 형식 오류 등 예외 발생 시 false 반환
//            return false;
//        }
//    }
//
//    // JWT 토큰 검증 - 토큰 만료 여부 확인
//    public boolean isTokenExpired(String token) {
//        try {
//            Claims claims = Jwts.parserBuilder()
//                    .setSigningKey(SECRET_KEY)
//                    .build()
//                    .parseClaimsJws(token)
//                    .getBody();
//
//            Date expiration = claims.getExpiration();
//            return expiration.before(new Date());  // true면 만료됨
//        } catch (JwtException | IllegalArgumentException e) {
//            // 토큰 파싱 실패(포맷 오류, 서명 불일치 등)도 만료로 간주할 수 있음
//            return true;
//        }
//    }

}

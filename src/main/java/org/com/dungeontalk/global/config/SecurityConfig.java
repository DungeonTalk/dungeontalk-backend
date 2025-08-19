package org.com.dungeontalk.global.config;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.global.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {

    // 공개 API를 한 곳에서 정의
    private static final String[] PUBLIC_URLS = {
            "/v1/member/register",
            "/v1/auth/login",
            "/v1/valkey/session/keys",
            "/v1/valkey/session/all",
            "/v1/auth/refresh",
            "/v1/valkey/session/test/save",
            "/v1/stat",
            "v1/match",
            "v1/rooms",
            "/v1/characters",
            "/init/",
            "/stat-calculator.html",
            "/character-test.html",
            "/dungeon-game.html",
            "/ws-chat",
            // Swagger UI 관련 경로들
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/swagger-resources",
            "dungeontalk-heartbeat.html"
    };

    public List<String> getPublicUrls() {
        return List.of(PUBLIC_URLS);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter)
            throws Exception {
        http
                // csrf 차단
                .csrf(AbstractHttpConfigurer::disable)

                // cors 설정
                .cors(cors -> cors.configurationSource(org.com.dungeontalk.global.config.CorsConfig.corsConfigurationSource()))

                // 시큐리티 기본 로그인 비활성화
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // 권한 url 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // ======================= DEPRECATED CODE - 3일간 관찰 한 후 문제 없으면 삭제 예정 =========================

//                .authorizeHttpRequests(req -> req.
//                        requestMatchers("/v1/member/register").permitAll().
//                        requestMatchers("/v1/auth/login").permitAll().
//                        requestMatchers("/v1/valkey/session/all").permitAll().
//                        requestMatchers("/v1/valkey/session/keys").permitAll().
//                        requestMatchers("/v1/auth/refresh").permitAll().
//                        requestMatchers("/v1/valkey/session/test/save").permitAll().
//
//                        requestMatchers("/swagger-ui/**").permitAll().
//                        requestMatchers("/swagger-ui/index.html").permitAll().
//                        requestMatchers("/v3/api-docs/**").permitAll().
//                        requestMatchers("/webjars/").permitAll().
//                        requestMatchers("/api/chat/room/**").authenticated().                   // 채팅방 생성/입장/퇴장은 인증 필요
//                        requestMatchers(HttpMethod.GET, "/api/chat/rooms").permitAll().        // 목록 조회는 공개
//                        requestMatchers(HttpMethod.GET, "/api/chat/room/**").permitAll().       // 단일 조회 및 메시지 조회 허용
//                        requestMatchers("/ws-chat/**", "/ws-chat").permitAll().                             // WebSocket 엔드포인트 허용 (HandshakeInterceptor에서 인증 처리)
//
//                        anyRequest().authenticated())
//                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 필터 추가

        return http.build();

    }

    /* todo : 이것으로 적용하기 */
//    @Bean
//    public PasswordEncoder passwordEncoder() {
//        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
//    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // ======================= 권한 설정 로직 =========================

    /* 아직 권한은 없으니, 보류 */

}

package org.com.dungeontalk.global.config;

import lombok.RequiredArgsConstructor;
import org.com.dungeontalk.global.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
@EnableMethodSecurity(securedEnabled = true, prePostEnabled = true)
public class SecurityConfig {
    
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring()
                .requestMatchers("/test-auth.html", "/debug-login.html", "/*.html")
                .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico");
    }

    // 공개 API를 한 곳에서 정의
    private static final String[] PUBLIC_URLS = {
            "/v1/member/register",
            "/v1/auth/login",
            "/v1/valkey/session/keys",
            "/v1/valkey/session/all",
            "/v1/auth/refresh",
            "/v1/valkey/session/test/save",
            "/v1/stat/**",
            "/v1/characters/**",
            "/init/**",
            "/stat-calculator.html",
            "/character-test.html",
            "/dungeon-game.html",
            "/ws-chat/**",
            // Swagger UI 관련 경로들
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/webjars/**",
            "/swagger-resources/**",
            "/dungeontalk-heartbeat.html",
            // 게임 관련 조회 API만 공개
            "/v1/match/queue-stats",
            "/v1/aichat/rooms/available",
            "/v1/worlds"  // 세계관 목록 조회
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
                
                // 로그아웃 설정 비활성화 (커스텀 로그아웃 사용)
                .logout(AbstractHttpConfigurer::disable)
                
                // 인증 실패 시 처리
                .exceptionHandling(exceptions -> exceptions
                    .authenticationEntryPoint((request, response, authException) -> {
                        String requestURI = request.getRequestURI();
                        
                        // 페이지 요청인 경우 로그인 페이지로 리다이렉트
                        if (requestURI.equals("/game") || 
                            requestURI.equals("/profile") || 
                            requestURI.equals("/settings")) {
                            response.sendRedirect("/login");
                        } else if (requestURI.startsWith("/v1/")) {
                            // API 요청인 경우 401 에러 반환
                            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");
                        } else {
                            // 기타 요청은 로그인 페이지로 리다이렉트
                            response.sendRedirect("/login");
                        }
                    })
                )
                
                // 권한 url 설정
                .authorizeHttpRequests(auth -> auth
                        // 정적 리소스 허용
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/*.html", "/test-auth.html").permitAll()
                        
                        // Thymeleaf 뷰 허용 (게임 페이지는 인증 필요)
                        .requestMatchers("/login", "/test", "/error", "/chat", "/profile", "/settings").permitAll()
                        .requestMatchers("/game").authenticated()
                        
                        // 서버사이드 로그인/로그아웃 추가
                        .requestMatchers("/v1/auth/server-login", "/v1/auth/server-logout").permitAll()
                        
                        // Swagger UI 관련
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        
                        // PUBLIC_URLS 배열 사용
                        .requestMatchers(PUBLIC_URLS).permitAll()
                        
                        // 나머지는 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

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

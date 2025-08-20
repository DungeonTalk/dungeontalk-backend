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

    // 공개 API 엔드포인트 (인증 없이 접근 가능)
    private static final String[] PUBLIC_API_URLS = {
            // 인증 관련
            "/v1/member/register",
            "/v1/auth/login",
            "/v1/auth/refresh",
            "/v1/auth/server-login",
            "/v1/auth/server-logout",
            
            // 테스트 및 개발용
            "/v1/valkey/session/keys",
            "/v1/valkey/session/all",
            "/v1/valkey/session/test/save",
            
            // 게임 데이터 조회 (읽기 전용)
            "/v1/stat/**",
            "/v1/characters/**",  // v1은 공개, v2는 인증 필요
            "/v1/match/queue-stats",
            "/v1/aichat/rooms/available",
            "/v1/worlds",
            
            // 초기화 및 WebSocket
            "/init/**",
            "/ws-chat/**"
    };
    
    // 인증이 필요한 API
    private static final String[] AUTHENTICATED_URLS = {
            "/v2/characters/**"  // v2 캐릭터 API는 인증 필요
    };

    public List<String> getPublicUrls() {
        return List.of(PUBLIC_API_URLS);
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
                        
                        // API 요청인 경우 401 에러 반환 (v1, v2 모두 포함)
                        if (requestURI.startsWith("/v1/") || requestURI.startsWith("/v2/")) {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"code\":\"401\",\"message\":\"Unauthorized\",\"data\":null}");
                        } 
                        // 페이지 요청인 경우 로그인 페이지로 리다이렉트
                        else if (requestURI.equals("/game") || 
                                 requestURI.equals("/profile") || 
                                 requestURI.equals("/settings")) {
                            response.sendRedirect("/login");
                        } 
                        // 기타 요청은 로그인 페이지로 리다이렉트
                        else {
                            response.sendRedirect("/login");
                        }
                    })
                )
                
                // 권한 url 설정
                .authorizeHttpRequests(auth -> auth
                        // 1. 정적 리소스 허용
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/*.html").permitAll()
                        
                        // 2. Thymeleaf 뷰 페이지
                        .requestMatchers("/login", "/test", "/error").permitAll()
                        .requestMatchers("/chat", "/profile", "/settings").permitAll()  // 추후 인증 필요시 변경
                        .requestMatchers("/game", "/game/play", "/game/character/**").authenticated()
                        
                        // 3. Swagger UI 문서
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/webjars/**").permitAll()
                        
                        // 4. 공개 API 엔드포인트
                        .requestMatchers(PUBLIC_API_URLS).permitAll()
                        
                        // 5. 인증 필요한 API
                        .requestMatchers(AUTHENTICATED_URLS).authenticated()
                        
                        // 6. 나머지 모든 요청은 인증 필요
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

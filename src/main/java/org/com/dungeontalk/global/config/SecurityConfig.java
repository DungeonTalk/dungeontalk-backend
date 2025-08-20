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
                .authorizeHttpRequests(req -> req
                        // 정적 리소스 허용
                        .requestMatchers("/", "/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/*.html", "/test-auth.html").permitAll()
                        
                        // Thymeleaf 뷰 허용 (게임 페이지는 인증 필요)
                        .requestMatchers("/login", "/test", "/error", "/chat", "/profile", "/settings").permitAll()
                        .requestMatchers("/game").authenticated()
                        
                        // 회원가입, 로그인, 로그아웃 API 허용
                        .requestMatchers("/v1/member/register").permitAll()
                        .requestMatchers("/v1/auth/login", "/v1/auth/server-login", "/v1/auth/logout", "/v1/auth/server-logout").permitAll()
                        .requestMatchers("/v1/valkey/session/all").permitAll()
                        
                        // Swagger UI 허용
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()
                        
                        // WebSocket 엔드포인트 허용
                        .requestMatchers("/ws-chat/**", "/ws-matching/**", "/ws-ai-chat/**").permitAll()
                        
                        // 나머지는 모두 허용 (개발 단계)
                        .anyRequest().permitAll())
                        
                // JWT 필터 활성화
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

  // 아직 권한은 없으니, 보류

}

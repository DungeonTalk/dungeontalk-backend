package org.com.dungeontalk.global.config;


import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;

@Configuration
@NoArgsConstructor
public class CorsConfig {
    @Bean
    public static CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        /* 리소스를 허용할 URL 파일 추가 */
        ArrayList<String> allowedOriginPatterns = new ArrayList<>();
        allowedOriginPatterns.add("http://localhost:8080");
        allowedOriginPatterns.add("http://localhost:5173");
        allowedOriginPatterns.add("http://localhost:5174");
        configuration.setAllowedOrigins(allowedOriginPatterns);

        /* 허용하는 HTTP METHOD 지정 */
        ArrayList<String> allowedHttpMethods = new ArrayList<>();
        allowedHttpMethods.add("GET");
        allowedHttpMethods.add("POST");
        allowedHttpMethods.add("PUT");
        allowedHttpMethods.add("DELETE");
        allowedHttpMethods.add("PATCH");
        allowedHttpMethods.add("OPTIONS");
        configuration.setAllowedMethods(allowedHttpMethods);

        // 인증, 인가를 위한 credentials 를 TRUE로 설정
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}

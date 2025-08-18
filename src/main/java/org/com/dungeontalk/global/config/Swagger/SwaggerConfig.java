package org.com.dungeontalk.global.config.Swagger;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger springdoc-ui 구성 파일
 */
@Configuration
public class SwaggerConfig {

    /* Swagger 설정 Bean */
    // 접근 경로 : http://localhost:8080/swagger-ui/index.html
    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Dungeon Talk Backend API")
                .version("v0.0.1")
                .description("F6 팀 API 명세서");

        return new OpenAPI()
                .components(new Components())
                .info(info);
    }
    /**
     * Swagger 보안 설정
     * JWT 토큰 인증을 위한 설정
     */
    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("JWT");
    }

    private Components components() {
        return new Components()
                .addSecuritySchemes("JWT", new SecurityScheme()
                        .name("JWT")
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .in(SecurityScheme.In.HEADER)
                        .description("JWT 토큰을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다."));
    }
}
package com.hip.damoa.config.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${swagger.server-url:http://localhost:8080}")
    private String serverUrl;

    @Bean
    public OpenAPI openAPI() {
        Info info = new Info()
                .title("Damoa API")
                .version("v1.0.0")
                .description("Damoa 프로젝트 API 명세서입니다.");

        // Security Scheme 설정 (JWT Bearer Token)
        SecurityScheme bearerAuth = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .in(SecurityScheme.In.HEADER)
                .name("Authorization");

        // 서버 URL 설정 (HTTPS 지원)
        Server server = new Server()
                .url(serverUrl)
                .description("API Server");

        // 전역 Security Requirement 제거 - 각 API에서 개별적으로 설정
        // Public API는 인증 불필요, Authenticated API만 @SecurityRequirement 추가

        return new OpenAPI()
                .servers(List.of(server))
                .components(new Components().addSecuritySchemes("bearerAuth", bearerAuth))
                .info(info);
    }
}

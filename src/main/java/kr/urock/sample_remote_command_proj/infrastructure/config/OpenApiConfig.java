package kr.urock.sample_remote_command_proj.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger) 설정
 *
 * API 문서 자동 생성 및 Swagger UI 제공
 * - Swagger UI: http://localhost:8080/swagger-ui.html
 * - API Docs JSON: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${spring.application.name}")
    private String applicationName;

    @Bean
    public OpenAPI customOpenAPI() {
        // Admin API Key 보안 스키마
        SecurityScheme adminApiKeyScheme = new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("X-Admin-Key")
            .description("관리자 API 키 (Admin API 전용)");

        // Client API Key 보안 스키마
        SecurityScheme clientApiKeyScheme = new SecurityScheme()
            .type(SecurityScheme.Type.APIKEY)
            .in(SecurityScheme.In.HEADER)
            .name("X-API-Key")
            .description("클라이언트 API 키 (Client API 전용)");

        // 보안 요구사항
        SecurityRequirement adminSecurityRequirement = new SecurityRequirement()
            .addList("Admin API Key");

        SecurityRequirement clientSecurityRequirement = new SecurityRequirement()
            .addList("Client API Key");

        return new OpenAPI()
            .info(new Info()
                .title("SSH Remote Command Execution API")
                .description("""
                    ## SSH 기반 원격 명령어 실행 시스템 API

                    이 API는 서버에서 Windows 클라이언트로 SSH를 통해 명령어를 전송하고 실행하는 시스템입니다.

                    ### 인증 방식
                    - **Admin API Key**: 관리자 전용 API (클라이언트 관리, 명령어 실행)
                    - **Client API Key**: 클라이언트 자가 등록 및 정보 조회

                    ### 주요 기능
                    - 클라이언트 등록 및 관리 (SSH 인증 정보 암호화)
                    - 명령어 화이트리스트/블랙리스트 검증
                    - 비동기 명령어 실행 (재시도 로직 포함)
                    - 명령어 실행 이력 조회

                    ### 보안 주의사항
                    - API Key는 환경변수로 관리하세요
                    - SSH 패스워드는 AES-256으로 암호화되어 저장됩니다
                    - 프로덕션 환경에서는 반드시 HTTPS를 사용하세요
                    """)
                .version("1.0.0")
                .contact(new Contact()
                    .name("API Support")
                    .email("support@example.com"))
                .license(new License()
                    .name("MIT License")
                    .url("https://opensource.org/licenses/MIT")))
            .servers(List.of(
                new Server()
                    .url("http://localhost:8080")
                    .description("로컬 개발 서버"),
                new Server()
                    .url("https://api.example.com")
                    .description("프로덕션 서버 (예시)")))
            .components(new Components()
                .addSecuritySchemes("Admin API Key", adminApiKeyScheme)
                .addSecuritySchemes("Client API Key", clientApiKeyScheme))
            .addSecurityItem(adminSecurityRequirement)
            .addSecurityItem(clientSecurityRequirement);
    }
}

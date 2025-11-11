package kr.urock.sample_remote_command_proj.infrastructure.config;

import kr.urock.sample_remote_command_proj.infrastructure.security.ApiKeyAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 설정
 *
 * API Key 기반 인증 사용
 * - Stateless (세션 미사용)
 * - CSRF 비활성화 (REST API)
 * - Method-level security 활성화 (@PreAuthorize 지원)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final ApiKeyAuthFilter apiKeyAuthFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (REST API)
            .csrf(AbstractHttpConfigurer::disable)

            // 세션 미사용 (Stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // 요청 권한 설정
            .authorizeHttpRequests(auth -> auth
                // Actuator는 인증 없이 접근 가능
                .requestMatchers("/actuator/**").permitAll()

                // 클라이언트 자가 등록은 인증 없이 접근 가능 (초기 등록)
                .requestMatchers("/api/clients/register").permitAll()

                // Swagger UI 및 API Docs는 인증 없이 접근 가능
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/v3/api-docs/**",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()

                // 나머지는 인증 필요
                .anyRequest().authenticated()
            )

            // API Key 필터 추가
            .addFilterBefore(apiKeyAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

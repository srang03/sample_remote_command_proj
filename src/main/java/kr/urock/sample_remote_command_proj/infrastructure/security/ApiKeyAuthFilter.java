package kr.urock.sample_remote_command_proj.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.urock.sample_remote_command_proj.domain.client.ClientCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * API Key 기반 인증 필터
 *
 * HTTP 헤더에서 API Key를 추출하여 인증 처리
 * - X-Admin-Key: 관리자 API Key
 * - X-API-Key: 클라이언트 API Key
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String ADMIN_KEY_HEADER = "X-Admin-Key";
    private static final String API_KEY_HEADER = "X-API-Key";

    private final ClientCredentialRepository clientCredentialRepository;

    @Value("${app.security.admin-api-key}")
    private String adminApiKey;

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {

        String requestPath = request.getRequestURI();

        // Actuator 엔드포인트는 인증 제외
        if (requestPath.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Admin API 인증
        String adminKey = request.getHeader(ADMIN_KEY_HEADER);
        if (adminKey != null) {
            if (adminKey.equals(adminApiKey)) {
                authenticateAsAdmin();
                filterChain.doFilter(request, response);
                return;
            } else {
                log.warn("Invalid admin API key attempt from IP: {}", request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid admin API key\"}");
                return;
            }
        }

        // Client API 인증
        String clientKey = request.getHeader(API_KEY_HEADER);
        if (clientKey != null) {
            var client = clientCredentialRepository.findByApiKey(clientKey);
            if (client.isPresent() && client.get().isActive()) {
                authenticateAsClient(clientKey);
                filterChain.doFilter(request, response);
                return;
            } else {
                log.warn("Invalid client API key attempt from IP: {}", request.getRemoteAddr());
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid or inactive API key\"}");
                return;
            }
        }

        // API Key가 없는 경우
        log.warn("Missing API key from IP: {}", request.getRemoteAddr());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("{\"error\": \"API key required\"}");
    }

    /**
     * 관리자로 인증
     */
    private void authenticateAsAdmin() {
        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
        var authentication = new UsernamePasswordAuthenticationToken(
            "admin",
            null,
            authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Authenticated as admin");
    }

    /**
     * 클라이언트로 인증
     */
    private void authenticateAsClient(String apiKey) {
        var authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_CLIENT"));
        var authentication = new UsernamePasswordAuthenticationToken(
            apiKey,
            null,
            authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        log.debug("Authenticated as client with API key: {}", apiKey.substring(0, 10) + "...");
    }
}

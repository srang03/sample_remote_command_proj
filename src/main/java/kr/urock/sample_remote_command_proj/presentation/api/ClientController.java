package kr.urock.sample_remote_command_proj.presentation.api;

import jakarta.validation.Valid;
import kr.urock.sample_remote_command_proj.domain.client.ClientCredential;
import kr.urock.sample_remote_command_proj.domain.client.ClientService;
import kr.urock.sample_remote_command_proj.presentation.api.dto.ClientResponse;
import kr.urock.sample_remote_command_proj.presentation.api.dto.RegisterClientRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Client API - 자가 등록 및 조회
 *
 * 클라이언트가 스스로 등록하고 정보를 조회하는 API
 */
@Slf4j
@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    /**
     * 클라이언트 자가 등록
     *
     * 인증 없이 접근 가능 (초기 등록용)
     */
    @PostMapping("/register")
    public ResponseEntity<ClientResponse> register(
        @Valid @RequestBody RegisterClientRequest request
    ) {
        ClientCredential credential = clientService.registerClient(
            request.getHost(),
            request.getPort(),
            request.getUsername(),
            request.getPassword(),
            request.getDescription()
        );

        log.info("Client self-registered: {} (API Key: {})",
            credential.getHost(), credential.getApiKey());

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ClientResponse.from(credential));
    }

    /**
     * 내 정보 조회
     *
     * API Key로 인증된 클라이언트 자신의 정보 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ClientResponse> getMyInfo(Authentication authentication) {
        String apiKey = authentication.getName();
        ClientCredential credential = clientService.getClientByApiKey(apiKey);
        return ResponseEntity.ok(ClientResponse.from(credential));
    }
}

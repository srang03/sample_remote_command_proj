package kr.urock.sample_remote_command_proj.presentation.api;

import jakarta.validation.Valid;
import kr.urock.sample_remote_command_proj.domain.client.ClientCredential;
import kr.urock.sample_remote_command_proj.domain.client.ClientService;
import kr.urock.sample_remote_command_proj.presentation.api.dto.ClientResponse;
import kr.urock.sample_remote_command_proj.presentation.api.dto.RegisterClientRequest;
import kr.urock.sample_remote_command_proj.presentation.api.dto.UpdateClientRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin API - 클라이언트 관리
 *
 * 관리자 권한이 필요한 클라이언트 관리 API
 * - 클라이언트 등록/조회/수정/삭제
 * - API Key 재발급
 * - 활성화/비활성화
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/clients")
@RequiredArgsConstructor
public class AdminClientController {

    private final ClientService clientService;

    /**
     * 클라이언트 등록
     */
    @PostMapping
    public ResponseEntity<ClientResponse> registerClient(
        @Valid @RequestBody RegisterClientRequest request
    ) {
        ClientCredential credential = clientService.registerClient(
            request.getHost(),
            request.getPort(),
            request.getUsername(),
            request.getPassword(),
            request.getDescription()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ClientResponse.from(credential));
    }

    /**
     * 전체 클라이언트 조회
     */
    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        List<ClientResponse> clients = clientService.getAllClients().stream()
            .map(ClientResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(clients);
    }

    /**
     * 클라이언트 조회 (ID)
     */
    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClient(@PathVariable Long id) {
        ClientCredential credential = clientService.getClient(id);
        return ResponseEntity.ok(ClientResponse.from(credential));
    }

    /**
     * 클라이언트 수정
     */
    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
        @PathVariable Long id,
        @Valid @RequestBody UpdateClientRequest request
    ) {
        ClientCredential credential = clientService.updateClient(
            id,
            request.getHost(),
            request.getPort(),
            request.getUsername(),
            request.getDescription()
        );

        return ResponseEntity.ok(ClientResponse.from(credential));
    }

    /**
     * API Key 재발급
     */
    @PostMapping("/{id}/regenerate-key")
    public ResponseEntity<ClientResponse> regenerateApiKey(@PathVariable Long id) {
        String newApiKey = clientService.regenerateApiKey(id);
        ClientCredential credential = clientService.getClient(id);
        return ResponseEntity.ok(ClientResponse.from(credential));
    }

    /**
     * 클라이언트 활성화
     */
    @PostMapping("/{id}/activate")
    public ResponseEntity<Void> activateClient(@PathVariable Long id) {
        clientService.activateClient(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 클라이언트 비활성화
     */
    @PostMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivateClient(@PathVariable Long id) {
        clientService.deactivateClient(id);
        return ResponseEntity.ok().build();
    }

    /**
     * 클라이언트 삭제
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteClient(@PathVariable Long id) {
        clientService.deleteClient(id);
        return ResponseEntity.noContent().build();
    }
}

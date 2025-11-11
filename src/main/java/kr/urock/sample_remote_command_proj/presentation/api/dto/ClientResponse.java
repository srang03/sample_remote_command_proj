package kr.urock.sample_remote_command_proj.presentation.api.dto;

import kr.urock.sample_remote_command_proj.domain.client.ClientCredential;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 클라이언트 응답 DTO
 */
@Data
@Builder
public class ClientResponse {

    private Long id;
    private String host;
    private Integer port;
    private String username;
    private String apiKey;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime lastConnectedAt;

    /**
     * Entity를 DTO로 변환
     */
    public static ClientResponse from(ClientCredential credential) {
        return ClientResponse.builder()
            .id(credential.getId())
            .host(credential.getHost())
            .port(credential.getPort())
            .username(credential.getUsername())
            .apiKey(credential.getApiKey())
            .description(credential.getDescription())
            .active(credential.getActive())
            .createdAt(credential.getCreatedAt())
            .lastConnectedAt(credential.getLastConnectedAt())
            .build();
    }
}

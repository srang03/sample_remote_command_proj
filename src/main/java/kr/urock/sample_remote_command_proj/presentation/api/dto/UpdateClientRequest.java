package kr.urock.sample_remote_command_proj.presentation.api.dto;

import lombok.Data;

/**
 * 클라이언트 수정 요청 DTO
 */
@Data
public class UpdateClientRequest {

    private String host;
    private Integer port;
    private String username;
    private String description;
}

package kr.urock.sample_remote_command_proj.presentation.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 명령어 실행 요청 DTO
 */
@Data
public class ExecuteCommandRequest {

    @NotBlank(message = "Target host is required")
    private String targetHost;

    @NotBlank(message = "Command is required")
    private String command;
}

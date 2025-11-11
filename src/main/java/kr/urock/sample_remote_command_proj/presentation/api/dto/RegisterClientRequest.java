package kr.urock.sample_remote_command_proj.presentation.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 클라이언트 등록 요청 DTO
 */
@Data
public class RegisterClientRequest {

    @NotBlank(message = "Host is required")
    private String host;

    @Min(value = 1, message = "Port must be greater than 0")
    private Integer port = 22;

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    private String description;
}

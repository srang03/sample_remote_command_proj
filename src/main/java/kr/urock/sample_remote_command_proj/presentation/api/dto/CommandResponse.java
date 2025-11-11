package kr.urock.sample_remote_command_proj.presentation.api.dto;

import kr.urock.sample_remote_command_proj.domain.command.Command;
import kr.urock.sample_remote_command_proj.domain.command.CommandStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 명령어 응답 DTO
 */
@Data
@Builder
public class CommandResponse {

    private Long id;
    private String targetHost;
    private String command;
    private CommandStatus status;
    private String result;
    private String errorMessage;
    private Integer exitCode;
    private LocalDateTime createdAt;
    private LocalDateTime executedAt;
    private LocalDateTime completedAt;
    private Long executionDurationMs;

    /**
     * Entity를 DTO로 변환
     */
    public static CommandResponse from(Command command) {
        return CommandResponse.builder()
            .id(command.getId())
            .targetHost(command.getTargetHost())
            .command(command.getCommandText())
            .status(command.getStatus())
            .result(command.getResult())
            .errorMessage(command.getErrorMessage())
            .exitCode(command.getExitCode())
            .createdAt(command.getCreatedAt())
            .executedAt(command.getExecutedAt())
            .completedAt(command.getCompletedAt())
            .executionDurationMs(command.getExecutionDurationMs())
            .build();
    }
}

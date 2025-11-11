package kr.urock.sample_remote_command_proj.application.executor.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 명령어 실행 결과
 *
 * 불변 객체 (캡슐화)
 */
@Getter
@Builder
public class CommandResult {

    private final Long commandId;
    private final boolean success;
    private final String output;
    private final String errorOutput;
    private final Integer exitCode;
    private final String errorMessage;

    /**
     * 성공 결과 생성
     */
    public static CommandResult success(Long commandId, String output, String errorOutput, Integer exitCode) {
        return CommandResult.builder()
            .commandId(commandId)
            .success(true)
            .output(output)
            .errorOutput(errorOutput)
            .exitCode(exitCode)
            .build();
    }

    /**
     * 실패 결과 생성
     */
    public static CommandResult failure(Long commandId, String errorMessage) {
        return CommandResult.builder()
            .commandId(commandId)
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}

package kr.urock.sample_remote_command_proj.application.ssh.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * SSH 명령어 실행 결과
 *
 * 불변 객체 (캡슐화)
 */
@Getter
@Builder
public class SshExecutionResult {

    private final String output;
    private final String errorOutput;
    private final Integer exitCode;
    private final boolean success;
    private final String errorMessage;

    /**
     * 성공 결과 생성
     */
    public static SshExecutionResult success(String output, String errorOutput, Integer exitCode) {
        return SshExecutionResult.builder()
            .output(output)
            .errorOutput(errorOutput)
            .exitCode(exitCode)
            .success(true)
            .build();
    }

    /**
     * 실패 결과 생성
     */
    public static SshExecutionResult failure(String errorMessage) {
        return SshExecutionResult.builder()
            .success(false)
            .errorMessage(errorMessage)
            .build();
    }
}

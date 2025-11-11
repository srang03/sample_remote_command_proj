package kr.urock.sample_remote_command_proj.application.executor.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 명령어 실행 요청
 *
 * 불변 객체 (캡슐화)
 */
@Getter
@Builder
public class CommandRequest {

    private final Long commandId;
    private final String targetHost;
    private final String command;
    private final String apiKey;

    /**
     * 요청 생성
     */
    public static CommandRequest of(Long commandId, String targetHost, String command, String apiKey) {
        return CommandRequest.builder()
            .commandId(commandId)
            .targetHost(targetHost)
            .command(command)
            .apiKey(apiKey)
            .build();
    }
}

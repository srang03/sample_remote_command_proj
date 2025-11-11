package kr.urock.sample_remote_command_proj.application.ssh.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * SSH 연결 정보
 *
 * 불변 객체 (캡슐화)
 */
@Getter
@Builder
public class ConnectionInfo {

    private final String host;
    private final Integer port;
    private final String username;
    private final String password;
    private final Integer connectTimeoutSeconds;
    private final Integer commandTimeoutSeconds;

    /**
     * 기본 설정으로 생성
     */
    public static ConnectionInfo of(String host, Integer port, String username, String password) {
        return ConnectionInfo.builder()
            .host(host)
            .port(port != null ? port : 22)
            .username(username)
            .password(password)
            .connectTimeoutSeconds(10)
            .commandTimeoutSeconds(60)
            .build();
    }
}

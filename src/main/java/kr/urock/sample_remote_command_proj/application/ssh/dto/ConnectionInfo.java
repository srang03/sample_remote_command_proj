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
    private final String outputEncoding;  // SSH 명령어 출력 인코딩

    /**
     * 기본 설정으로 생성
     */
    public static ConnectionInfo of(String host, Integer port, String username, String password) {
        return of(host, port, username, password, "UTF-8");
    }

    /**
     * 인코딩을 지정하여 생성
     */
    public static ConnectionInfo of(String host, Integer port, String username, String password, String outputEncoding) {
        return ConnectionInfo.builder()
            .host(host)
            .port(port != null ? port : 22)
            .username(username)
            .password(password)
            .connectTimeoutSeconds(10)
            .commandTimeoutSeconds(60)
            .outputEncoding(outputEncoding != null ? outputEncoding : "UTF-8")
            .build();
    }
}

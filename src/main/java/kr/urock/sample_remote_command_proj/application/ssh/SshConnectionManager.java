package kr.urock.sample_remote_command_proj.application.ssh;

import kr.urock.sample_remote_command_proj.application.ssh.dto.ConnectionInfo;
import kr.urock.sample_remote_command_proj.application.ssh.dto.SshExecutionResult;

/**
 * SSH 연결 관리 인터페이스
 *
 * SSH 연결 생성, 명령어 실행, 연결 종료 담당
 * - sshj 기반 구현
 * - 추후 다른 SSH 라이브러리로 교체 가능
 */
public interface SshConnectionManager {

    /**
     * SSH를 통해 명령어 실행
     *
     * @param connectionInfo 연결 정보
     * @param command 실행할 명령어
     * @return 실행 결과
     */
    SshExecutionResult executeCommand(ConnectionInfo connectionInfo, String command);

    /**
     * SSH 연결 테스트
     *
     * @param connectionInfo 연결 정보
     * @return 연결 성공 여부
     */
    boolean testConnection(ConnectionInfo connectionInfo);
}

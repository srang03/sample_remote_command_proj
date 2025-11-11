package kr.urock.sample_remote_command_proj.application.ssh;

import kr.urock.sample_remote_command_proj.application.ssh.dto.ConnectionInfo;
import kr.urock.sample_remote_command_proj.application.ssh.dto.SshExecutionResult;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.common.IOUtils;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * sshj 기반 SSH 연결 관리자
 *
 * SSH 연결 및 명령어 실행
 * - 연결 실패 시 재시도 (Exponential Backoff)
 * - 타임아웃 설정 지원
 */
@Slf4j
@Component
public class SshjConnectionManager implements SshConnectionManager {

    private final int maxRetryAttempts;
    private final long retryBackoffMs;

    public SshjConnectionManager(
        @Value("${app.ssh.retry.max-attempts}") int maxRetryAttempts,
        @Value("${app.ssh.retry.backoff-ms}") long retryBackoffMs
    ) {
        this.maxRetryAttempts = maxRetryAttempts;
        this.retryBackoffMs = retryBackoffMs;
    }

    @Override
    public SshExecutionResult executeCommand(ConnectionInfo connectionInfo, String command) {
        SSHClient ssh = null;
        int attempt = 0;

        while (attempt < maxRetryAttempts) {
            attempt++;
            try {
                ssh = connectWithRetry(connectionInfo, attempt);
                return executeCommandInternal(ssh, command, connectionInfo.getCommandTimeoutSeconds());
            } catch (Exception e) {
                log.error("SSH command execution failed (attempt {}/{}): {}",
                    attempt, maxRetryAttempts, e.getMessage());

                // 연결 종료
                if (ssh != null && ssh.isConnected()) {
                    try {
                        ssh.disconnect();
                    } catch (IOException ex) {
                        log.warn("Failed to disconnect SSH: {}", ex.getMessage());
                    }
                }

                // 재시도 여부 결정
                if (attempt < maxRetryAttempts && isRetryableException(e)) {
                    long backoff = retryBackoffMs * (1L << (attempt - 1)); // Exponential backoff
                    log.info("Retrying in {} ms...", backoff);
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return SshExecutionResult.failure("Command execution interrupted");
                    }
                } else {
                    return SshExecutionResult.failure(e.getMessage());
                }
            } finally {
                if (ssh != null && ssh.isConnected()) {
                    try {
                        ssh.disconnect();
                    } catch (IOException e) {
                        log.warn("Failed to disconnect SSH: {}", e.getMessage());
                    }
                }
            }
        }

        return SshExecutionResult.failure("Max retry attempts exceeded");
    }

    @Override
    public boolean testConnection(ConnectionInfo connectionInfo) {
        SSHClient ssh = null;
        try {
            ssh = connectWithRetry(connectionInfo, 1);
            return ssh.isConnected() && ssh.isAuthenticated();
        } catch (Exception e) {
            log.error("SSH connection test failed: {}", e.getMessage());
            return false;
        } finally {
            if (ssh != null && ssh.isConnected()) {
                try {
                    ssh.disconnect();
                } catch (IOException e) {
                    log.warn("Failed to disconnect SSH: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * SSH 연결 시도
     */
    private SSHClient connectWithRetry(ConnectionInfo info, int attempt) throws IOException {
        SSHClient ssh = new SSHClient();

        // 호스트 키 검증 비활성화 (POC용, 운영에서는 적절한 검증 필요)
        ssh.addHostKeyVerifier(new PromiscuousVerifier());

        // 타임아웃 설정
        ssh.setConnectTimeout(info.getConnectTimeoutSeconds() * 1000);
        ssh.setTimeout(info.getCommandTimeoutSeconds() * 1000);

        log.debug("Attempting SSH connection to {}:{} (attempt {})",
            info.getHost(), info.getPort(), attempt);

        ssh.connect(info.getHost(), info.getPort());
        ssh.authPassword(info.getUsername(), info.getPassword());

        log.info("SSH connection established to {}:{}", info.getHost(), info.getPort());
        return ssh;
    }

    /**
     * 명령어 실행 (내부)
     */
    private SshExecutionResult executeCommandInternal(
        SSHClient ssh,
        String command,
        Integer timeoutSeconds
    ) throws IOException {
        Session session = null;
        try {
            session = ssh.startSession();
            Session.Command cmd = session.exec(command);

            // 타임아웃 대기
            cmd.join(timeoutSeconds, TimeUnit.SECONDS);

            // 결과 읽기
            String output = IOUtils.readFully(cmd.getInputStream()).toString();
            String errorOutput = IOUtils.readFully(cmd.getErrorStream()).toString();
            Integer exitCode = cmd.getExitStatus();

            log.debug("Command executed successfully. Exit code: {}", exitCode);

            return SshExecutionResult.success(output, errorOutput, exitCode);

        } finally {
            if (session != null) {
                try {
                    session.close();
                } catch (IOException e) {
                    log.warn("Failed to close SSH session: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * 재시도 가능한 예외인지 확인
     *
     * 연결 실패만 재시도, 인증 실패는 재시도하지 않음
     */
    private boolean isRetryableException(Exception e) {
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        // 인증 실패는 재시도 안함
        if (message.contains("auth") || message.contains("password")) {
            return false;
        }

        // 연결 관련 오류는 재시도
        return message.contains("connection") ||
               message.contains("timeout") ||
               message.contains("network") ||
               e instanceof IOException;
    }
}

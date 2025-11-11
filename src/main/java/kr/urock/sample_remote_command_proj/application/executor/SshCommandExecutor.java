package kr.urock.sample_remote_command_proj.application.executor;

import kr.urock.sample_remote_command_proj.application.executor.dto.CommandRequest;
import kr.urock.sample_remote_command_proj.application.executor.dto.CommandResult;
import kr.urock.sample_remote_command_proj.application.ssh.SshConnectionManager;
import kr.urock.sample_remote_command_proj.application.ssh.dto.ConnectionInfo;
import kr.urock.sample_remote_command_proj.application.ssh.dto.SshExecutionResult;
import kr.urock.sample_remote_command_proj.domain.client.ClientCredential;
import kr.urock.sample_remote_command_proj.domain.client.ClientCredentialRepository;
import kr.urock.sample_remote_command_proj.infrastructure.util.PasswordEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * SSH 기반 명령어 실행기
 *
 * SSH를 통해 원격 서버에 명령어 실행
 * - 비동기 실행 (@Async)
 * - 클라이언트 인증 정보 조회
 * - SSH 연결 관리자 사용
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SshCommandExecutor implements CommandExecutor {

    private final SshConnectionManager sshConnectionManager;
    private final ClientCredentialRepository clientCredentialRepository;
    private final PasswordEncryptor passwordEncryptor;

    @Value("${app.ssh.timeout-seconds}")
    private int timeoutSeconds;

    @Value("${app.ssh.connect-timeout-seconds}")
    private int connectTimeoutSeconds;

    @Override
    @Async
    public CompletableFuture<CommandResult> execute(CommandRequest request) {
        log.info("Executing command [id={}] on host [{}]: {}",
            request.getCommandId(), request.getTargetHost(), request.getCommand());

        try {
            // 클라이언트 인증 정보 조회
            ClientCredential credential = clientCredentialRepository.findByHost(request.getTargetHost())
                .orElseThrow(() -> new IllegalArgumentException(
                    "Client credential not found for host: " + request.getTargetHost()
                ));

            // 클라이언트 활성 상태 확인
            if (!credential.isActive()) {
                throw new IllegalStateException(
                    "Client is not active: " + request.getTargetHost()
                );
            }

            // 패스워드 복호화
            String decryptedPassword = passwordEncryptor.decrypt(credential.getEncryptedPassword());

            // 연결 정보 생성
            ConnectionInfo connectionInfo = ConnectionInfo.builder()
                .host(credential.getHost())
                .port(credential.getPort())
                .username(credential.getUsername())
                .password(decryptedPassword)
                .connectTimeoutSeconds(connectTimeoutSeconds)
                .commandTimeoutSeconds(timeoutSeconds)
                .build();

            // SSH 명령어 실행
            SshExecutionResult sshResult = sshConnectionManager.executeCommand(
                connectionInfo,
                request.getCommand()
            );

            // 마지막 연결 시간 업데이트
            credential.updateLastConnectedAt();
            clientCredentialRepository.save(credential);

            // 결과 변환
            if (sshResult.isSuccess()) {
                log.info("Command [id={}] completed successfully", request.getCommandId());
                return CompletableFuture.completedFuture(
                    CommandResult.success(
                        request.getCommandId(),
                        sshResult.getOutput(),
                        sshResult.getErrorOutput(),
                        sshResult.getExitCode()
                    )
                );
            } else {
                log.error("Command [id={}] failed: {}", request.getCommandId(), sshResult.getErrorMessage());
                return CompletableFuture.completedFuture(
                    CommandResult.failure(
                        request.getCommandId(),
                        sshResult.getErrorMessage()
                    )
                );
            }

        } catch (Exception e) {
            log.error("Command [id={}] execution error: {}", request.getCommandId(), e.getMessage(), e);
            return CompletableFuture.completedFuture(
                CommandResult.failure(
                    request.getCommandId(),
                    e.getMessage()
                )
            );
        }
    }
}

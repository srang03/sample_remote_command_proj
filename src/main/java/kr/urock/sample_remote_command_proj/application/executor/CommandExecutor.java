package kr.urock.sample_remote_command_proj.application.executor;

import kr.urock.sample_remote_command_proj.application.executor.dto.CommandRequest;
import kr.urock.sample_remote_command_proj.application.executor.dto.CommandResult;

import java.util.concurrent.CompletableFuture;

/**
 * 명령어 실행 인터페이스
 *
 * 실제 명령어 실행을 담당
 * - SSH 기반 구현
 * - 로컬 프로세스 기반 구현 (추후)
 * - 비동기 실행 지원
 */
public interface CommandExecutor {

    /**
     * 명령어 비동기 실행
     *
     * @param request 실행 요청
     * @return 실행 결과 (비동기)
     */
    CompletableFuture<CommandResult> execute(CommandRequest request);
}

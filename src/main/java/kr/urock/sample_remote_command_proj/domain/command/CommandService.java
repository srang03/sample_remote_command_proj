package kr.urock.sample_remote_command_proj.domain.command;

import kr.urock.sample_remote_command_proj.application.executor.CommandExecutor;
import kr.urock.sample_remote_command_proj.application.executor.dto.CommandRequest;
import kr.urock.sample_remote_command_proj.application.executor.dto.CommandResult;
import kr.urock.sample_remote_command_proj.application.validator.CommandValidator;
import kr.urock.sample_remote_command_proj.application.validator.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 명령어 도메인 서비스
 *
 * 명령어 실행의 오케스트레이션 담당
 * - 명령어 검증
 * - 명령어 실행 요청
 * - 명령어 상태 관리
 * - 명령어 이력 조회
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommandService {

    private final CommandRepository commandRepository;
    private final CommandValidator commandValidator;
    private final CommandExecutor commandExecutor;

    /**
     * 명령어 실행 요청
     *
     * @param targetHost 대상 호스트
     * @param commandText 명령어
     * @param apiKey API 키
     * @return 생성된 Command ID
     */
    @Transactional
    public Long executeCommand(String targetHost, String commandText, String apiKey) {
        // 1. 명령어 검증
        ValidationResult validationResult = commandValidator.validate(commandText);
        if (!validationResult.isValid()) {
            throw new IllegalArgumentException(
                "Command validation failed: " + validationResult.getReason()
            );
        }

        // 2. Command 엔티티 생성 및 저장
        Command command = Command.create(targetHost, commandText, apiKey);
        command = commandRepository.save(command);
        log.info("Command created [id={}]: {}", command.getId(), commandText);

        // 3. 비동기 실행
        final Long commandId = command.getId();
        CommandRequest request = CommandRequest.of(commandId, targetHost, commandText, apiKey);

        commandExecutor.execute(request)
            .thenAccept(result -> handleCommandResult(result))
            .exceptionally(throwable -> {
                handleCommandFailure(commandId, throwable.getMessage());
                return null;
            });

        return commandId;
    }

    /**
     * 명령어 조회
     *
     * @param commandId 명령어 ID
     * @return Command
     */
    public Command getCommand(Long commandId) {
        return commandRepository.findById(commandId)
            .orElseThrow(() -> new IllegalArgumentException("Command not found: " + commandId));
    }

    /**
     * 명령어 이력 조회 (페이징)
     *
     * @param pageable 페이징 정보
     * @return 명령어 목록
     */
    public Page<Command> getCommands(Pageable pageable) {
        return commandRepository.findAll(pageable);
    }

    /**
     * 상태별 명령어 조회
     *
     * @param status 상태
     * @param pageable 페이징 정보
     * @return 명령어 목록
     */
    public Page<Command> getCommandsByStatus(CommandStatus status, Pageable pageable) {
        return commandRepository.findByStatus(status, pageable);
    }

    /**
     * API Key로 명령어 조회
     *
     * @param apiKey API 키
     * @param pageable 페이징 정보
     * @return 명령어 목록
     */
    public Page<Command> getCommandsByApiKey(String apiKey, Pageable pageable) {
        return commandRepository.findByApiKey(apiKey, pageable);
    }

    /**
     * 대상 호스트로 명령어 조회
     *
     * @param targetHost 대상 호스트
     * @param pageable 페이징 정보
     * @return 명령어 목록
     */
    public Page<Command> getCommandsByTargetHost(String targetHost, Pageable pageable) {
        return commandRepository.findByTargetHost(targetHost, pageable);
    }

    /**
     * 대상 호스트와 상태로 명령어 조회
     *
     * @param targetHost 대상 호스트
     * @param status 상태
     * @param pageable 페이징 정보
     * @return 명령어 목록
     */
    public Page<Command> getCommandsByTargetHostAndStatus(String targetHost, CommandStatus status, Pageable pageable) {
        return commandRepository.findByTargetHostAndStatus(targetHost, status, pageable);
    }

    /**
     * 명령어 실행 결과 처리
     *
     * @param result 실행 결과
     */
    @Transactional
    public void handleCommandResult(CommandResult result) {
        Command command = commandRepository.findById(result.getCommandId())
            .orElseThrow(() -> new IllegalStateException(
                "Command not found: " + result.getCommandId()
            ));

        // 실행 중으로 상태 변경
        if (command.getStatus() == CommandStatus.PENDING) {
            command.markAsExecuting();
        }

        // 결과에 따라 상태 업데이트
        if (result.isSuccess()) {
            String fullOutput = result.getOutput();
            if (result.getErrorOutput() != null && !result.getErrorOutput().isEmpty()) {
                fullOutput += "\n[STDERR]\n" + result.getErrorOutput();
            }
            command.markAsSuccess(fullOutput, result.getExitCode());
            log.info("Command [id={}] completed successfully", command.getId());
        } else {
            command.markAsFailed(result.getErrorMessage());
            log.error("Command [id={}] failed: {}", command.getId(), result.getErrorMessage());
        }

        commandRepository.save(command);
    }

    /**
     * 명령어 실행 실패 처리
     *
     * @param commandId 명령어 ID
     * @param errorMessage 오류 메시지
     */
    @Transactional
    public void handleCommandFailure(Long commandId, String errorMessage) {
        Command command = commandRepository.findById(commandId)
            .orElseThrow(() -> new IllegalStateException("Command not found: " + commandId));

        if (command.getStatus() == CommandStatus.PENDING) {
            command.markAsExecuting();
        }

        command.markAsFailed(errorMessage);
        commandRepository.save(command);
        log.error("Command [id={}] failed: {}", commandId, errorMessage);
    }
}

package kr.urock.sample_remote_command_proj.domain.command;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 명령어 실행 이력을 나타내는 도메인 엔티티
 *
 * 캡슐화 원칙:
 * - 모든 필드는 private으로 선언
 * - 상태 변경은 도메인 메서드를 통해서만 가능
 * - 외부에서 직접 필드 수정 불가
 */
@Entity
@Table(name = "commands", indexes = {
    @Index(name = "idx_command_status", columnList = "status"),
    @Index(name = "idx_command_created_at", columnList = "created_at"),
    @Index(name = "idx_command_target_host", columnList = "target_host")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Command {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_host", nullable = false, length = 255)
    private String targetHost;

    @Column(name = "command_text", nullable = false, length = 2000)
    private String commandText;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CommandStatus status;

    @Column(name = "result", columnDefinition = "TEXT")
    private String result;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(name = "api_key", nullable = false, length = 255)
    private String apiKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "executed_at")
    private LocalDateTime executedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "execution_duration_ms")
    private Long executionDurationMs;

    /**
     * 새로운 명령어 생성
     */
    public static Command create(String targetHost, String commandText, String apiKey) {
        Command command = new Command();
        command.targetHost = targetHost;
        command.commandText = commandText;
        command.apiKey = apiKey;
        command.status = CommandStatus.PENDING;
        command.createdAt = LocalDateTime.now();
        return command;
    }

    /**
     * 명령어 실행 시작
     *
     * @throws IllegalStateException PENDING 상태가 아닌 경우
     */
    public void markAsExecuting() {
        if (this.status != CommandStatus.PENDING) {
            throw new IllegalStateException(
                String.format("Cannot execute command in %s status. Expected: PENDING", this.status)
            );
        }
        this.status = CommandStatus.EXECUTING;
        this.executedAt = LocalDateTime.now();
    }

    /**
     * 명령어 실행 성공
     *
     * @param result 실행 결과
     * @param exitCode 종료 코드
     * @throws IllegalStateException EXECUTING 상태가 아닌 경우
     */
    public void markAsSuccess(String result, Integer exitCode) {
        if (this.status != CommandStatus.EXECUTING) {
            throw new IllegalStateException(
                String.format("Cannot complete command in %s status. Expected: EXECUTING", this.status)
            );
        }
        this.status = CommandStatus.SUCCESS;
        this.result = result;
        this.exitCode = exitCode;
        this.completedAt = LocalDateTime.now();
        calculateDuration();
    }

    /**
     * 명령어 실행 실패
     *
     * @param errorMessage 오류 메시지
     * @throws IllegalStateException EXECUTING 상태가 아닌 경우
     */
    public void markAsFailed(String errorMessage) {
        if (this.status != CommandStatus.EXECUTING) {
            throw new IllegalStateException(
                String.format("Cannot fail command in %s status. Expected: EXECUTING", this.status)
            );
        }
        this.status = CommandStatus.FAILED;
        this.errorMessage = errorMessage;
        this.completedAt = LocalDateTime.now();
        calculateDuration();
    }

    /**
     * 명령어 타임아웃
     *
     * @throws IllegalStateException EXECUTING 상태가 아닌 경우
     */
    public void markAsTimeout() {
        if (this.status != CommandStatus.EXECUTING) {
            throw new IllegalStateException(
                String.format("Cannot timeout command in %s status. Expected: EXECUTING", this.status)
            );
        }
        this.status = CommandStatus.TIMEOUT;
        this.errorMessage = "Command execution timeout";
        this.completedAt = LocalDateTime.now();
        calculateDuration();
    }

    /**
     * 실행 시간 계산 (내부 메서드)
     */
    private void calculateDuration() {
        if (this.executedAt != null && this.completedAt != null) {
            this.executionDurationMs = java.time.Duration.between(
                this.executedAt,
                this.completedAt
            ).toMillis();
        }
    }

    /**
     * 명령어 실행 완료 여부
     */
    public boolean isCompleted() {
        return this.status == CommandStatus.SUCCESS
            || this.status == CommandStatus.FAILED
            || this.status == CommandStatus.TIMEOUT;
    }

    /**
     * 명령어 실행 성공 여부
     */
    public boolean isSuccess() {
        return this.status == CommandStatus.SUCCESS;
    }
}

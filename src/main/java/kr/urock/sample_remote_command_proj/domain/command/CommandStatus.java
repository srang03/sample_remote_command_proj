package kr.urock.sample_remote_command_proj.domain.command;

/**
 * 명령어 실행 상태를 나타내는 열거형
 */
public enum CommandStatus {
    /**
     * 대기 중 - 명령어가 생성되었으나 아직 실행되지 않음
     */
    PENDING,

    /**
     * 실행 중 - 명령어가 현재 실행 중
     */
    EXECUTING,

    /**
     * 성공 - 명령어가 성공적으로 실행됨
     */
    SUCCESS,

    /**
     * 실패 - 명령어 실행 중 오류 발생
     */
    FAILED,

    /**
     * 타임아웃 - 명령어 실행 시간 초과
     */
    TIMEOUT
}

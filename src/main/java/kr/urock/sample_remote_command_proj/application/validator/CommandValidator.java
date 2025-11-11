package kr.urock.sample_remote_command_proj.application.validator;

/**
 * 명령어 검증 인터페이스
 *
 * 명령어가 실행 가능한지 검증
 * - 화이트리스트 기반 허용
 * - 블랙리스트 기반 차단
 */
public interface CommandValidator {

    /**
     * 명령어 검증
     *
     * @param command 검증할 명령어
     * @return 검증 결과
     */
    ValidationResult validate(String command);
}

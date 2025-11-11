package kr.urock.sample_remote_command_proj.application.validator;

import lombok.Getter;

/**
 * 명령어 검증 결과
 *
 * 불변 객체로 설계 (캡슐화)
 */
@Getter
public class ValidationResult {

    private final boolean valid;
    private final String reason;

    private ValidationResult(boolean valid, String reason) {
        this.valid = valid;
        this.reason = reason;
    }

    /**
     * 허용된 명령어
     */
    public static ValidationResult allowed() {
        return new ValidationResult(true, null);
    }

    /**
     * 거부된 명령어
     *
     * @param reason 거부 사유
     */
    public static ValidationResult rejected(String reason) {
        return new ValidationResult(false, reason);
    }

    /**
     * 성공 여부
     */
    public boolean isValid() {
        return valid;
    }
}

package kr.urock.sample_remote_command_proj.application.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * 파일 기반 명령어 검증기
 *
 * 검증 우선순위:
 * 1. 화이트리스트 매칭 → 허용
 * 2. 블랙리스트 매칭 → 거부
 * 3. 둘 다 매칭 안됨 → 거부 (기본 거부 정책)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FileBasedCommandValidator implements CommandValidator {

    private final CommandPolicyLoader policyLoader;

    @Override
    public ValidationResult validate(String command) {
        if (command == null || command.trim().isEmpty()) {
            return ValidationResult.rejected("Command cannot be empty");
        }

        String trimmedCommand = command.trim();

        // 1. 화이트리스트 확인
        if (matchesAnyPattern(trimmedCommand, policyLoader.getWhitelist())) {
            log.debug("Command allowed by whitelist: {}", trimmedCommand);
            return ValidationResult.allowed();
        }

        // 2. 블랙리스트 확인
        if (matchesAnyPattern(trimmedCommand, policyLoader.getBlacklist())) {
            log.warn("Command rejected by blacklist: {}", trimmedCommand);
            return ValidationResult.rejected("Command matches blacklist pattern");
        }

        // 3. 기본 거부
        log.warn("Command rejected (not in whitelist): {}", trimmedCommand);
        return ValidationResult.rejected("Command not in whitelist");
    }

    /**
     * 명령어가 패턴 집합 중 하나라도 매칭되는지 확인
     *
     * @param command 명령어
     * @param patterns 패턴 집합 (정규식)
     * @return 매칭 여부
     */
    private boolean matchesAnyPattern(String command, Set<String> patterns) {
        for (String patternStr : patterns) {
            try {
                Pattern pattern = Pattern.compile(patternStr);
                if (pattern.matcher(command).matches()) {
                    return true;
                }
            } catch (Exception e) {
                log.error("Invalid regex pattern: {}", patternStr, e);
            }
        }
        return false;
    }
}

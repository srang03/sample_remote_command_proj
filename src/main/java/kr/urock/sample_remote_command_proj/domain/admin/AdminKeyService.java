package kr.urock.sample_remote_command_proj.domain.admin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Admin API Key 관리 서비스
 *
 * Admin API Key 조회, 재발급, 검증
 */
@Slf4j
@Service
public class AdminKeyService {

    @Value("${app.security.admin-api-key}")
    private String adminApiKey;

    // 런타임에 재발급된 키를 저장 (서버 재시작 시 초기화됨)
    private String runtimeAdminKey = null;

    /**
     * 현재 유효한 Admin API Key 반환 (마스킹)
     *
     * @return 마스킹된 Admin API Key
     */
    public String getMaskedAdminKey() {
        String currentKey = getCurrentAdminKey();

        if (currentKey.length() <= 8) {
            return "****";
        }

        // 처음 4자리 + **** + 마지막 4자리
        return currentKey.substring(0, 4) + "****" +
               currentKey.substring(currentKey.length() - 4);
    }

    /**
     * Admin API Key 재발급
     *
     * 런타임 키를 생성하고 기존 키를 무효화
     * 서버 재시작 시 application.yml의 키로 돌아감
     *
     * @return 새로운 Admin API Key
     */
    public String regenerateAdminKey() {
        String newKey = "admin-" + UUID.randomUUID().toString();
        this.runtimeAdminKey = newKey;

        log.warn("Admin API Key regenerated. New key: {}****",
            newKey.substring(0, 10));

        return newKey;
    }

    /**
     * Admin API Key 검증
     *
     * @param keyToValidate 검증할 키
     * @return 유효 여부
     */
    public boolean validateAdminKey(String keyToValidate) {
        if (keyToValidate == null || keyToValidate.isEmpty()) {
            return false;
        }

        return keyToValidate.equals(getCurrentAdminKey());
    }

    /**
     * 현재 유효한 Admin API Key 조회
     *
     * 런타임 키가 있으면 런타임 키, 없으면 설정 파일의 키
     *
     * @return 현재 유효한 Admin API Key
     */
    public String getCurrentAdminKey() {
        return (runtimeAdminKey != null) ? runtimeAdminKey : adminApiKey;
    }
}

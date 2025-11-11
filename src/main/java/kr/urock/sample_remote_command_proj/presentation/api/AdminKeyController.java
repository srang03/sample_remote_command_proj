package kr.urock.sample_remote_command_proj.presentation.api;

import kr.urock.sample_remote_command_proj.domain.admin.AdminKeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Admin API Key 관리 컨트롤러
 *
 * Admin API Key 조회 및 재발급
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/api-key")
@RequiredArgsConstructor
public class AdminKeyController {

    private final AdminKeyService adminKeyService;

    /**
     * 현재 Admin API Key 정보 조회 (마스킹)
     *
     * 보안을 위해 전체 키가 아닌 일부만 표시
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentAdminKey() {
        String maskedKey = adminKeyService.getMaskedAdminKey();

        Map<String, Object> response = new HashMap<>();
        response.put("adminApiKey", maskedKey);
        response.put("message", "Full key is masked for security. Check application.yml or environment variable.");

        return ResponseEntity.ok(response);
    }

    /**
     * Admin API Key 재발급
     *
     * ⚠️ 주의: 기존 키는 즉시 무효화됩니다
     * 환경변수 ADMIN_API_KEY를 업데이트해야 서버 재시작 후에도 유지됩니다
     */
    @PostMapping("/regenerate")
    public ResponseEntity<Map<String, Object>> regenerateAdminKey() {
        String newKey = adminKeyService.regenerateAdminKey();

        Map<String, Object> response = new HashMap<>();
        response.put("newAdminApiKey", newKey);
        response.put("warning", "Update ADMIN_API_KEY environment variable to persist after restart");

        log.warn("Admin API Key regenerated. Old key is now invalid.");

        return ResponseEntity.ok(response);
    }

    /**
     * Admin API Key 검증
     *
     * 제공된 키가 유효한지 확인
     */
    @PostMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateAdminKey(
        @RequestBody Map<String, String> request
    ) {
        String keyToValidate = request.get("apiKey");
        boolean isValid = adminKeyService.validateAdminKey(keyToValidate);

        Map<String, Object> response = new HashMap<>();
        response.put("valid", isValid);

        return ResponseEntity.ok(response);
    }
}

package kr.urock.sample_remote_command_proj.domain.client;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 클라이언트 SSH 인증 정보를 나타내는 도메인 엔티티
 *
 * 캡슐화 원칙:
 * - 모든 필드는 private으로 선언
 * - 민감 정보(패스워드)는 암호화되어 저장
 * - 외부에서 직접 필드 수정 불가
 */
@Entity
@Table(name = "client_credentials", indexes = {
    @Index(name = "idx_client_host", columnList = "host"),
    @Index(name = "idx_client_api_key", columnList = "api_key", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClientCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "host", nullable = false, length = 255)
    private String host;

    @Column(name = "port", nullable = false)
    private Integer port;

    @Column(name = "username", nullable = false, length = 255)
    private String username;

    /**
     * 암호화된 패스워드
     * 실제 저장 전에 암호화되어야 함
     */
    @Column(name = "encrypted_password", nullable = false, length = 500)
    private String encryptedPassword;

    /**
     * API Key - 클라이언트 식별 및 인증용
     */
    @Column(name = "api_key", nullable = false, unique = true, length = 255)
    private String apiKey;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_connected_at")
    private LocalDateTime lastConnectedAt;

    /**
     * 새로운 클라이언트 인증 정보 생성
     *
     * @param host 호스트 주소
     * @param port SSH 포트
     * @param username SSH 사용자명
     * @param encryptedPassword 암호화된 패스워드
     * @param description 설명
     * @return 새로운 ClientCredential 인스턴스
     */
    public static ClientCredential create(
        String host,
        Integer port,
        String username,
        String encryptedPassword,
        String description
    ) {
        ClientCredential credential = new ClientCredential();
        credential.host = host;
        credential.port = port != null ? port : 22; // 기본 SSH 포트
        credential.username = username;
        credential.encryptedPassword = encryptedPassword;
        credential.apiKey = generateApiKey();
        credential.description = description;
        credential.active = true;
        credential.createdAt = LocalDateTime.now();
        return credential;
    }

    /**
     * API Key 생성
     */
    private static String generateApiKey() {
        return "client-" + UUID.randomUUID().toString();
    }

    /**
     * 패스워드 업데이트 (암호화된 패스워드로)
     *
     * @param newEncryptedPassword 새로운 암호화된 패스워드
     */
    public void updatePassword(String newEncryptedPassword) {
        this.encryptedPassword = newEncryptedPassword;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 클라이언트 정보 업데이트
     *
     * @param host 호스트 주소
     * @param port SSH 포트
     * @param username SSH 사용자명
     * @param description 설명
     */
    public void updateInfo(String host, Integer port, String username, String description) {
        if (host != null) {
            this.host = host;
        }
        if (port != null) {
            this.port = port;
        }
        if (username != null) {
            this.username = username;
        }
        if (description != null) {
            this.description = description;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * API Key 재발급
     *
     * @return 새로운 API Key
     */
    public String regenerateApiKey() {
        this.apiKey = generateApiKey();
        this.updatedAt = LocalDateTime.now();
        return this.apiKey;
    }

    /**
     * 클라이언트 활성화
     */
    public void activate() {
        this.active = true;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 클라이언트 비활성화
     */
    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 마지막 연결 시간 업데이트
     */
    public void updateLastConnectedAt() {
        this.lastConnectedAt = LocalDateTime.now();
    }

    /**
     * 클라이언트 활성 상태 확인
     */
    public boolean isActive() {
        return this.active != null && this.active;
    }
}

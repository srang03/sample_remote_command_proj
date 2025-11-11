package kr.urock.sample_remote_command_proj.domain.client;

import kr.urock.sample_remote_command_proj.infrastructure.util.PasswordEncryptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 클라이언트 도메인 서비스
 *
 * 클라이언트 인증 정보 관리
 * - 클라이언트 등록
 * - 클라이언트 조회
 * - 클라이언트 수정/삭제
 * - API Key 재발급
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClientService {

    private final ClientCredentialRepository clientCredentialRepository;
    private final PasswordEncryptor passwordEncryptor;

    /**
     * 클라이언트 등록
     *
     * @param host 호스트
     * @param port 포트
     * @param username 사용자명
     * @param plainPassword 평문 패스워드
     * @param description 설명
     * @return 생성된 ClientCredential (API Key 포함)
     */
    @Transactional
    public ClientCredential registerClient(
        String host,
        Integer port,
        String username,
        String plainPassword,
        String description
    ) {
        // 중복 확인
        if (clientCredentialRepository.existsByHost(host)) {
            throw new IllegalArgumentException("Client with host already exists: " + host);
        }

        // 패스워드 암호화
        String encryptedPassword = passwordEncryptor.encrypt(plainPassword);

        // 클라이언트 생성
        ClientCredential credential = ClientCredential.create(
            host,
            port,
            username,
            encryptedPassword,
            description
        );

        credential = clientCredentialRepository.save(credential);
        log.info("Client registered [id={}]: {} (API Key: {})",
            credential.getId(), credential.getHost(), credential.getApiKey());

        return credential;
    }

    /**
     * 클라이언트 조회 (ID)
     *
     * @param clientId 클라이언트 ID
     * @return ClientCredential
     */
    public ClientCredential getClient(Long clientId) {
        return clientCredentialRepository.findById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
    }

    /**
     * 클라이언트 조회 (API Key)
     *
     * @param apiKey API Key
     * @return ClientCredential
     */
    public ClientCredential getClientByApiKey(String apiKey) {
        return clientCredentialRepository.findByApiKey(apiKey)
            .orElseThrow(() -> new IllegalArgumentException("Client not found for API key"));
    }

    /**
     * 클라이언트 조회 (Host)
     *
     * @param host 호스트
     * @return ClientCredential
     */
    public ClientCredential getClientByHost(String host) {
        return clientCredentialRepository.findByHost(host)
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + host));
    }

    /**
     * 전체 클라이언트 조회
     *
     * @return 클라이언트 목록
     */
    public List<ClientCredential> getAllClients() {
        return clientCredentialRepository.findAll();
    }

    /**
     * 활성 클라이언트 조회
     *
     * @return 활성 클라이언트 목록
     */
    public List<ClientCredential> getActiveClients() {
        return clientCredentialRepository.findByActive(true);
    }

    /**
     * 클라이언트 정보 수정
     *
     * @param clientId 클라이언트 ID
     * @param host 호스트 (nullable)
     * @param port 포트 (nullable)
     * @param username 사용자명 (nullable)
     * @param description 설명 (nullable)
     * @return 수정된 ClientCredential
     */
    @Transactional
    public ClientCredential updateClient(
        Long clientId,
        String host,
        Integer port,
        String username,
        String description
    ) {
        ClientCredential credential = getClient(clientId);
        credential.updateInfo(host, port, username, description);

        credential = clientCredentialRepository.save(credential);
        log.info("Client updated [id={}]: {}", credential.getId(), credential.getHost());

        return credential;
    }

    /**
     * 클라이언트 패스워드 수정
     *
     * @param clientId 클라이언트 ID
     * @param newPlainPassword 새 평문 패스워드
     * @return 수정된 ClientCredential
     */
    @Transactional
    public ClientCredential updateClientPassword(Long clientId, String newPlainPassword) {
        ClientCredential credential = getClient(clientId);

        String encryptedPassword = passwordEncryptor.encrypt(newPlainPassword);
        credential.updatePassword(encryptedPassword);

        credential = clientCredentialRepository.save(credential);
        log.info("Client password updated [id={}]", credential.getId());

        return credential;
    }

    /**
     * API Key 재발급
     *
     * @param clientId 클라이언트 ID
     * @return 새로운 API Key
     */
    @Transactional
    public String regenerateApiKey(Long clientId) {
        ClientCredential credential = getClient(clientId);
        String newApiKey = credential.regenerateApiKey();

        clientCredentialRepository.save(credential);
        log.info("Client API key regenerated [id={}]: {}", credential.getId(), newApiKey);

        return newApiKey;
    }

    /**
     * 클라이언트 활성화
     *
     * @param clientId 클라이언트 ID
     */
    @Transactional
    public void activateClient(Long clientId) {
        ClientCredential credential = getClient(clientId);
        credential.activate();
        clientCredentialRepository.save(credential);
        log.info("Client activated [id={}]", credential.getId());
    }

    /**
     * 클라이언트 비활성화
     *
     * @param clientId 클라이언트 ID
     */
    @Transactional
    public void deactivateClient(Long clientId) {
        ClientCredential credential = getClient(clientId);
        credential.deactivate();
        clientCredentialRepository.save(credential);
        log.info("Client deactivated [id={}]", credential.getId());
    }

    /**
     * 클라이언트 삭제
     *
     * @param clientId 클라이언트 ID
     */
    @Transactional
    public void deleteClient(Long clientId) {
        ClientCredential credential = getClient(clientId);
        clientCredentialRepository.delete(credential);
        log.info("Client deleted [id={}]: {}", credential.getId(), credential.getHost());
    }
}

package kr.urock.sample_remote_command_proj.domain.client;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * ClientCredential 엔티티의 데이터 액세스 인터페이스
 */
@Repository
public interface ClientCredentialRepository extends JpaRepository<ClientCredential, Long> {

    /**
     * API Key로 클라이언트 조회
     */
    Optional<ClientCredential> findByApiKey(String apiKey);

    /**
     * 호스트로 클라이언트 조회
     */
    Optional<ClientCredential> findByHost(String host);

    /**
     * 호스트와 포트로 클라이언트 조회
     */
    Optional<ClientCredential> findByHostAndPort(String host, Integer port);

    /**
     * 활성 상태 클라이언트 조회
     */
    List<ClientCredential> findByActive(Boolean active);

    /**
     * API Key 존재 여부 확인
     */
    boolean existsByApiKey(String apiKey);

    /**
     * 호스트 존재 여부 확인
     */
    boolean existsByHost(String host);
}

package kr.urock.sample_remote_command_proj.domain.command;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Command 엔티티의 데이터 액세스 인터페이스
 */
@Repository
public interface CommandRepository extends JpaRepository<Command, Long> {

    /**
     * 상태별 명령어 조회 (페이징)
     */
    Page<Command> findByStatus(CommandStatus status, Pageable pageable);

    /**
     * 대상 호스트별 명령어 조회 (페이징)
     */
    Page<Command> findByTargetHost(String targetHost, Pageable pageable);

    /**
     * API Key로 명령어 조회 (페이징)
     */
    Page<Command> findByApiKey(String apiKey, Pageable pageable);

    /**
     * 생성일 기준 명령어 조회
     */
    List<Command> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    /**
     * 특정 상태의 오래된 명령어 조회 (타임아웃 처리용)
     */
    @Query("SELECT c FROM Command c WHERE c.status = :status AND c.executedAt < :beforeTime")
    List<Command> findOldExecutingCommands(
        @Param("status") CommandStatus status,
        @Param("beforeTime") LocalDateTime beforeTime
    );

    /**
     * 대상 호스트와 상태로 명령어 개수 조회
     */
    long countByTargetHostAndStatus(String targetHost, CommandStatus status);

    /**
     * 대상 호스트와 상태로 명령어 조회 (페이징)
     */
    Page<Command> findByTargetHostAndStatus(String targetHost, CommandStatus status, Pageable pageable);
}

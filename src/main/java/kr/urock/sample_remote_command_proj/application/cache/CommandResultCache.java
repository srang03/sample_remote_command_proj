package kr.urock.sample_remote_command_proj.application.cache;

import kr.urock.sample_remote_command_proj.application.executor.dto.CommandResult;

import java.util.Optional;

/**
 * 명령어 실행 결과 캐시 인터페이스
 *
 * 캐싱 전략 추상화
 * - NoOp 구현 (캐싱 안함)
 * - Redis 구현 (추후)
 * - 메모리 캐시 구현 (추후)
 */
public interface CommandResultCache {

    /**
     * 캐시에서 결과 조회
     *
     * @param key 캐시 키
     * @return 캐시된 결과 (Optional)
     */
    Optional<CommandResult> get(String key);

    /**
     * 캐시에 결과 저장
     *
     * @param key 캐시 키
     * @param result 결과
     */
    void put(String key, CommandResult result);

    /**
     * 캐시 무효화
     *
     * @param key 캐시 키
     */
    void invalidate(String key);

    /**
     * 전체 캐시 초기화
     */
    void clear();
}

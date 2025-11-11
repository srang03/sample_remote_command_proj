package kr.urock.sample_remote_command_proj.application.cache;

import kr.urock.sample_remote_command_proj.application.executor.dto.CommandResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * No-Operation 캐시 구현
 *
 * 실제로는 캐싱하지 않음 (POC용)
 * - 항상 Optional.empty() 반환
 * - 추후 Redis 또는 다른 캐시 구현으로 교체 가능
 */
@Slf4j
@Component
public class NoOpCommandResultCache implements CommandResultCache {

    @Override
    public Optional<CommandResult> get(String key) {
        log.trace("Cache get (NoOp): {}", key);
        return Optional.empty();
    }

    @Override
    public void put(String key, CommandResult result) {
        log.trace("Cache put (NoOp): {}", key);
        // No operation
    }

    @Override
    public void invalidate(String key) {
        log.trace("Cache invalidate (NoOp): {}", key);
        // No operation
    }

    @Override
    public void clear() {
        log.trace("Cache clear (NoOp)");
        // No operation
    }
}

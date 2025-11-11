package kr.urock.sample_remote_command_proj.application.validator;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 파일 기반 명령어 정책 로더
 *
 * 화이트리스트/블랙리스트를 파일에서 로드
 * - 주기적으로 파일 변경 확인 및 리로드
 * - Thread-safe 구현 (ReadWriteLock)
 */
@Slf4j
@Component
public class FileBasedPolicyLoader implements CommandPolicyLoader {

    private final ResourceLoader resourceLoader;
    private final String whitelistPath;
    private final String blacklistPath;
    private final boolean reloadEnabled;

    private Set<String> whitelist;
    private Set<String> blacklist;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public FileBasedPolicyLoader(
        ResourceLoader resourceLoader,
        @Value("${app.command.whitelist-path}") String whitelistPath,
        @Value("${app.command.blacklist-path}") String blacklistPath,
        @Value("${app.command.policy-reload-enabled}") boolean reloadEnabled
    ) {
        this.resourceLoader = resourceLoader;
        this.whitelistPath = whitelistPath;
        this.blacklistPath = blacklistPath;
        this.reloadEnabled = reloadEnabled;
        this.whitelist = new HashSet<>();
        this.blacklist = new HashSet<>();
    }

    /**
     * 초기 로드
     */
    @PostConstruct
    public void init() {
        reload();
        log.info("Command policy loaded. Whitelist: {} patterns, Blacklist: {} patterns",
            whitelist.size(), blacklist.size());
    }

    /**
     * 주기적 리로드 (5초마다, 설정에서 활성화된 경우)
     */
    @Scheduled(fixedDelayString = "${app.command.policy-check-interval-ms}")
    public void scheduledReload() {
        if (reloadEnabled) {
            reload();
        }
    }

    @Override
    public Set<String> getWhitelist() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableSet(new HashSet<>(whitelist));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Set<String> getBlacklist() {
        lock.readLock().lock();
        try {
            return Collections.unmodifiableSet(new HashSet<>(blacklist));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void reload() {
        lock.writeLock().lock();
        try {
            Set<String> newWhitelist = loadPatternsFromFile(whitelistPath);
            Set<String> newBlacklist = loadPatternsFromFile(blacklistPath);

            this.whitelist = newWhitelist;
            this.blacklist = newBlacklist;

            log.debug("Policy reloaded. Whitelist: {} patterns, Blacklist: {} patterns",
                whitelist.size(), blacklist.size());
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 파일에서 패턴 로드
     *
     * @param path 파일 경로
     * @return 패턴 집합
     */
    private Set<String> loadPatternsFromFile(String path) {
        Set<String> patterns = new HashSet<>();

        try {
            Resource resource = resourceLoader.getResource(path);
            if (!resource.exists()) {
                log.warn("Policy file not found: {}", path);
                return patterns;
            }

            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    // 빈 줄 또는 주석 무시
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }

                    patterns.add(line);
                }
            }

            log.debug("Loaded {} patterns from {}", patterns.size(), path);
        } catch (IOException e) {
            log.error("Failed to load policy file: {}", path, e);
        }

        return patterns;
    }
}

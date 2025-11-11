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

    // 파일 변경 감지를 위한 타임스탬프
    private long whitelistLastModified = 0L;
    private long blacklistLastModified = 0L;

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
     * 파일이 실제로 변경된 경우에만 리로드 수행
     */
    @Scheduled(fixedDelayString = "${app.command.policy-check-interval-ms}")
    public void scheduledReload() {
        if (!reloadEnabled) {
            return;
        }

        // 파일 변경 감지
        boolean whitelistChanged = hasFileChanged(whitelistPath, whitelistLastModified);
        boolean blacklistChanged = hasFileChanged(blacklistPath, blacklistLastModified);

        if (whitelistChanged || blacklistChanged) {
            log.info("Policy file changes detected. Reloading...");
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

            // 타임스탬프 업데이트
            this.whitelistLastModified = getFileLastModified(whitelistPath);
            this.blacklistLastModified = getFileLastModified(blacklistPath);

            log.info("Policy reloaded. Whitelist: {} patterns, Blacklist: {} patterns",
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

    /**
     * 파일 변경 여부 확인
     *
     * @param path 파일 경로
     * @param lastModified 마지막 수정 시간
     * @return 파일이 변경되었으면 true
     */
    private boolean hasFileChanged(String path, long lastModified) {
        long currentModified = getFileLastModified(path);
        return currentModified > lastModified;
    }

    /**
     * 파일의 마지막 수정 시간 조회
     *
     * @param path 파일 경로
     * @return 마지막 수정 시간 (밀리초), 파일이 없으면 0
     */
    private long getFileLastModified(String path) {
        try {
            Resource resource = resourceLoader.getResource(path);
            if (resource.exists()) {
                return resource.lastModified();
            }
        } catch (IOException e) {
            log.warn("Failed to get last modified time for: {}", path);
        }
        return 0L;
    }
}

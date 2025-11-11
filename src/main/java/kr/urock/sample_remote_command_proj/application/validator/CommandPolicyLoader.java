package kr.urock.sample_remote_command_proj.application.validator;

import java.util.Set;

/**
 * 명령어 정책 로더 인터페이스
 *
 * 화이트리스트 및 블랙리스트를 로드하고 관리
 * - 파일 기반 구현 가능
 * - DB 기반 구현 가능
 * - 런타임 리로드 지원
 */
public interface CommandPolicyLoader {

    /**
     * 화이트리스트 조회
     *
     * @return 허용 명령어 패턴 집합 (정규식)
     */
    Set<String> getWhitelist();

    /**
     * 블랙리스트 조회
     *
     * @return 차단 명령어 패턴 집합 (정규식)
     */
    Set<String> getBlacklist();

    /**
     * 정책 리로드
     *
     * 파일 또는 DB에서 정책을 다시 로드
     */
    void reload();
}

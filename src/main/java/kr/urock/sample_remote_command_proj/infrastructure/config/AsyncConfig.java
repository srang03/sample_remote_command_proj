package kr.urock.sample_remote_command_proj.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 비동기 실행 설정
 *
 * 명령어 실행을 비동기로 처리하기 위한 Thread Pool 설정
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 코어 스레드 수
        executor.setCorePoolSize(5);

        // 최대 스레드 수
        executor.setMaxPoolSize(10);

        // 큐 용량
        executor.setQueueCapacity(25);

        // 스레드 이름 접두사
        executor.setThreadNamePrefix("CommandExecutor-");

        // 종료 대기
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();
        return executor;
    }
}

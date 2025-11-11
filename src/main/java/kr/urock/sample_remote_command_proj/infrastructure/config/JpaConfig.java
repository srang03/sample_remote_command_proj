package kr.urock.sample_remote_command_proj.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * JPA 설정
 */
@Configuration
@EnableJpaRepositories(basePackages = "kr.urock.sample_remote_command_proj.domain")
@EnableTransactionManagement
@EnableJpaAuditing
public class JpaConfig {
}

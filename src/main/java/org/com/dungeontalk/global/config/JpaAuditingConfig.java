package org.com.dungeontalk.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing
@ConditionalOnClass(JpaRepository.class)    // classpath 에 JPA가 있을 때만
@Profile("!test")                           // test profile 에서는 비활성화
public class JpaAuditingConfig {

}

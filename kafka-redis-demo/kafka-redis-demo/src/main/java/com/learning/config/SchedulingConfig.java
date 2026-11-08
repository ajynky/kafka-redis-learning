package com.learning.config;

import javax.sql.DataSource;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Slf4j
@EnableScheduling
@EnableSchedulerLock(defaultLockAtMostFor = "30s")
@Configuration
public class SchedulingConfig {

    @Bean
    public LockProvider lockProvider(@Qualifier("postgresDataSource") DataSource db) {
        return new JdbcTemplateLockProvider(config(db));
    }

    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
        threadPoolTaskScheduler.setThreadNamePrefix("scheduling-");
        threadPoolTaskScheduler.setPoolSize(2);
        threadPoolTaskScheduler.setErrorHandler(t -> log.warn("Scheduled task failed: {}", t.getMessage()));
        return threadPoolTaskScheduler;
    }

    private static JdbcTemplateLockProvider.@NonNull Configuration config(DataSource db) {
        return new JdbcTemplateLockProvider.Configuration.Builder()
                .withJdbcTemplate(new JdbcTemplate(db))
                .usingDbTime()
                .build();
    }
}

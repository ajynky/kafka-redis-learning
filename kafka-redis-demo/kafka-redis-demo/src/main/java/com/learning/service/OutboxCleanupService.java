package com.learning.service;

import com.learning.repository.OutboxRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@RequiredArgsConstructor
@Service
public class OutboxCleanupService {

    @Value("${app.outbox.retention}")
    private Duration retention;

    private final OutboxRepository repository;

    @Scheduled(cron = "${app.outbox.cleanup-cron}")
    @SchedulerLock(name = "outboxCleanup", lockAtMostFor = "10m")
    @Transactional
    public void deletePublishedOutboxEvents() {
        Instant cutOff = Instant.now().minus(retention);
        int count = repository.deletePublishedBefore(cutOff);
        log.info("Outbox cleanup deleted {} rows older than {}", count, cutOff);
    }
}

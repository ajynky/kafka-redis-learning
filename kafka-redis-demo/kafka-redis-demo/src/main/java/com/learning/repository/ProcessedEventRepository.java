package com.learning.repository;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@RequiredArgsConstructor
public class ProcessedEventRepository {
    private static final String PREFIX = "processed:";
    private static final Duration TTL = Duration.ofDays(7);
    private final StringRedisTemplate template;

    private String keyBuilder(String group, String eventId) {
        return PREFIX + group + ":" + eventId;
    }

    public boolean isProcessed(String group, String eventId) {
        String key = keyBuilder(group, eventId);
        if (Boolean.TRUE.equals(template.hasKey(key))) {
            log.debug("Redis Exists {} -> {}", key, "found");
            return true;
        }
        log.debug("Redis Exists {} -> {}", key, "missing");
        return false;
    }

    public void markProcessed(String groupId, String eventId) {
        String key = keyBuilder(groupId, eventId);
        template.opsForValue().set(key, "1", TTL);
        log.debug("Redis SET {} (ttl {})", key, TTL);
    }
}

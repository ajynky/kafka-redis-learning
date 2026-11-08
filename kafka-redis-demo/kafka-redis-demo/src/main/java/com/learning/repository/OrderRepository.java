package com.learning.repository;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class OrderRepository {
    private static final String STATUS_PREFIX = "order-status:";
    private static final String ORDER_PREFIX = "order:";
    private final StringRedisTemplate redisTemplate;

    public void save(String orderId, String value) {
        String key = ORDER_PREFIX + orderId;
        setValue(value, key);
    }

    public Optional<String> read(String orderId) {
        String key = ORDER_PREFIX + orderId;
        return getValue(key);
    }

    private void setValue(String value, String key) {
        redisTemplate.opsForValue().set(key, value);
        log.debug("Redis SET {}", key);
    }

    private @NonNull Optional<String> getValue(String key) {
        Optional<String> value = Optional.ofNullable(redisTemplate.opsForValue().get(key));
        log.debug("Redis GET {} -> {}", key, value.isPresent() ? "found" : "missing");
        return value;
    }

    public void saveStatus(String orderId, String status) {
        String key = STATUS_PREFIX + orderId;
        setValue(status, key);
    }

    public Optional<String> findStatus(String orderId) {
        String key = STATUS_PREFIX + orderId;
        return getValue(key);
    }

    public void saveStatusIfAbsent(String orderId, String status) {
        String key = STATUS_PREFIX + orderId;
        Boolean written = redisTemplate.opsForValue().setIfAbsent(key, status);
        log.debug(
                "Redis SETNX {} -> {}",
                key,
                Boolean.TRUE.equals(written) ? "written" : "skipped (status already exists)");
    }
}

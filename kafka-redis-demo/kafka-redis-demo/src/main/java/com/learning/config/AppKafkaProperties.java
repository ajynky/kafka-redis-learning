package com.learning.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka")
public record AppKafkaProperties(Topics topics, Retry retry) {
    public record Topics(Topic orders, Topic ordersDlt, Topic payments) {}

    public record Topic(String name, int partitions) {}

    public record Retry(long interval, int maxRetries) {}
}

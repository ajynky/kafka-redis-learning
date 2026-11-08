package com.learning.config;

import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    private final AppKafkaProperties kafkaProperties;

    @Bean
    public NewTopic paymentsTopic() {
        return newTopic(kafkaProperties.topics().payments());
    }

    @Bean
    public NewTopic ordersTopic() {
        return newTopic(kafkaProperties.topics().orders());
    }

    @Bean
    public NewTopic ordersDltTopic() {
        return newTopic(kafkaProperties.topics().ordersDlt());
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(
                        kafkaProperties.retry().interval(),
                        kafkaProperties.retry().maxRetries()));
    }

    private NewTopic newTopic(AppKafkaProperties.Topic topic) {
        return TopicBuilder.name(topic.name())
                .partitions(topic.partitions())
                .replicas(1)
                .build();
    }
}

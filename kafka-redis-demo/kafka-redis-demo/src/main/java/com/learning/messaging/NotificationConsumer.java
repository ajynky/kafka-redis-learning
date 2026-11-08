package com.learning.messaging;

import static com.learning.model.PaymentStatus.*;

import com.learning.model.PaymentStatus;
import com.learning.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class NotificationConsumer {
    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.payments.name}",
            groupId = "${app.kafka.consumers.notification.group-id}",
            clientIdPrefix = "notification-${server.port:8080}")
    public void readMessage(ConsumerRecord<String, String> record) {
        log.debug(
                "Key {}, Value {}, Partition {}, Offset {}",
                record.key(),
                record.value(),
                record.partition(),
                record.offset());
        String key = record.key();
        if (record.key() == null || record.key().isBlank()) {
            log.warn(
                    "Status cannot be formed for order has no key, partition {} offset {}",
                    record.partition(),
                    record.offset());
            return;
        }
        PaymentStatus status = from(record.value());
        switch (status) {
            case FAILED -> {
                log.info("Notify customer: payment {} for order {}", status, key);
                orderRepository.saveStatus(key, status.name());
            }
            case PAID -> {
                log.info("Notify customer: order {} confirmed", key);
                orderRepository.saveStatus(key, status.name());
            }
            case null, default -> log.warn("Unknown payment status {} for order {}", record.value(), key);
        }
    }
}

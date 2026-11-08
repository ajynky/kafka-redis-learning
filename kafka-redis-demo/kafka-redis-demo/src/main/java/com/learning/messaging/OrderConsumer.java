package com.learning.messaging;

import com.learning.model.OrderStatus;
import com.learning.repository.OrderRepository;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderConsumer {

    private final OrderRepository orderRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.orders.name}",
            groupId = "${app.kafka.consumers.order.group-id}",
            clientIdPrefix = "order-${server.port:8080}")
    public void readMessage(ConsumerRecord<String, String> record) {
        String key = record.key();
        log.debug(
                "Key {}, Value {}, Partition {}, Offset {}",
                record.key(),
                record.value(),
                record.partition(),
                record.offset());

        if (record.key() == null || record.key().isBlank()) {
            log.warn("Skipping order as it has no key, partition {} offset {}", record.partition(), record.offset());
            return;
        }
        orderRepository.save(key, Objects.requireNonNullElse(record.value(), ""));
        orderRepository.saveStatusIfAbsent(key, OrderStatus.CREATED.name());
    }
}

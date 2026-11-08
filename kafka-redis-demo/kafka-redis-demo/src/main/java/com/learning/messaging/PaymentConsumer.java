package com.learning.messaging;

import com.learning.config.AppKafkaProperties;
import com.learning.model.PaymentStatus;
import com.learning.repository.ProcessedEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentConsumer {
    private final AppKafkaProperties props;
    private final KafkaPublisher kafkaPublisher;

    @Value("${app.kafka.consumers.payment.group-id}")
    private String groupId;

    private final ProcessedEventRepository eventRepository;

    @KafkaListener(
            topics = "${app.kafka.topics.orders.name}",
            groupId = "${app.kafka.consumers.payment.group-id}",
            clientIdPrefix = "payment-${server.port:8080}")
    public void readMessage(
            ConsumerRecord<String, String> record,
            @Header(name = KafkaPublisher.EVENT_ID, required = false) String eventId) {
        log.debug(
                "Key {}, Value {}, Partition {}, Offset {}",
                record.key(),
                record.value(),
                record.partition(),
                record.offset());
        String key = record.key();
        if (record.key() == null || record.key().isBlank()) {
            log.warn("Skipping payment: order has no key, partition {} offset {}", record.partition(), record.offset());
            return;
        }
        if (eventId != null && eventRepository.isProcessed(groupId, eventId)) {
            log.debug("Skip duplicate eventId={}, key {}", eventId, key);
            return;
        }
        String value = record.value();
        String status = (value == null || value.isBlank() || value.contains("fail"))
                ? PaymentStatus.FAILED.name()
                : PaymentStatus.PAID.name();
        log.info("Payment {} for order {}", status, key);
        kafkaPublisher.publish(props.topics().payments().name(), key, status);

        if (eventId != null) {
            eventRepository.markProcessed(groupId, eventId);
        }
    }
}

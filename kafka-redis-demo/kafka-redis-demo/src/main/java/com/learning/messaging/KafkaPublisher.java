package com.learning.messaging;

import com.learning.exception.PublishException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class KafkaPublisher {

    public static final String EVENT_ID = "eventId";
    private final KafkaTemplate<String, String> template;

    public void publish(String topicName, String key, String value) {
        log.debug("Publishing to topic={} key={}", topicName, key);
        try {
            SendResult<String, String> result =
                    template.send(topicName, key, value).get(5, TimeUnit.SECONDS);
            logDetails(topicName, key, result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublishException(topicName, key, e);
        } catch (KafkaException | TimeoutException | ExecutionException e) {
            throw new PublishException(topicName, key, e);
        }
    }

    public void publishFromRelay(String topicName, String key, String value, long eventId) {
        try {
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, key, value);
            producerRecord.headers().add(EVENT_ID, Long.toString(eventId).getBytes(StandardCharsets.UTF_8));
            SendResult<String, String> result = template.send(producerRecord).get(5, TimeUnit.SECONDS);
            logDetails(topicName, key, result);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new PublishException(topicName, key, e);
        } catch (KafkaException | TimeoutException | ExecutionException e) {
            throw new PublishException(topicName, key, e);
        }
    }

    private static void logDetails(String topicName, String key, SendResult<String, String> result) {
        log.info(
                "Published to topic={} key={} partition={} offset={}",
                topicName,
                key,
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }
}

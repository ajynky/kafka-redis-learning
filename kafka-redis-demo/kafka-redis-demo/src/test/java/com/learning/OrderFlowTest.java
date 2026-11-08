package com.learning;

import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.learning.messaging.KafkaPublisher;
import com.learning.repository.OrderRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Disabled
@EmbeddedKafka(
        partitions = 3,
        topics = {"orders"})
public class OrderFlowTest {
    @MockitoBean
    OrderRepository orderRepository;

    @Autowired
    KafkaPublisher kafkaPublisher;

    @Test
    void orderSentToKafkaIsSavedToRepository() {
        kafkaPublisher.publish("orders", "1", "Laptop");
        verify(orderRepository, timeout(5000)).save("1", "Laptop");
    }
}

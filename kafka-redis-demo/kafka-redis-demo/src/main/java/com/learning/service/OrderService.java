package com.learning.service;

import com.learning.config.AppKafkaProperties;
import com.learning.entity.OrderEntity;
import com.learning.entity.OutboxEvent;
import com.learning.exception.OrderAlreadyExistsException;
import com.learning.repository.OrderEntityRepository;
import com.learning.repository.OutboxRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderEntityRepository orderEntityRepository;
    private final OutboxRepository outboxRepository;
    private final AppKafkaProperties props;
    private final Tracer tracer;

    @Transactional
    public void createOrder(String orderId, String orderData) {

        if (orderEntityRepository.existsById(orderId)) {
            throw new OrderAlreadyExistsException(orderId);
        }
        OrderEntity orderTobeSaved = OrderEntity.builder()
                .orderId(orderId)
                .orderData(orderData)
                .createdAt(Instant.now())
                .build();
        orderEntityRepository.save(orderTobeSaved);
        String traceParent = getTraceParent();
        OutboxEvent eventTobeSaved = OutboxEvent.builder()
                .topic(props.topics().orders().name())
                .messageKey(orderId)
                .payload(orderData)
                .createdAt(Instant.now())
                .publishedAt(null)
                .traceParent(traceParent)
                .build();
        OutboxEvent savedEvent = outboxRepository.save(eventTobeSaved);
        log.info("Order saved orderId={} eventId={}", orderId, savedEvent.getId());
    }

    private @Nullable String getTraceParent() {
        String traceparent = null;
        Span span = tracer.currentSpan();
        if (span != null) {
            traceparent =
                    "00-" + span.context().traceId() + "-" + span.context().spanId() + "-01";
        }
        return traceparent;
    }
}

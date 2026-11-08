package com.learning.service;

import com.learning.entity.OutboxEvent;
import com.learning.exception.PublishException;
import com.learning.messaging.KafkaPublisher;
import com.learning.repository.OutboxRepository;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.annotation.SpanAspect;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OutboxRelayService {
    private final OutboxRepository outboxRepository;
    private final KafkaPublisher publisher;
    private final Tracer tracer;

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval}")
    @SchedulerLock(name = "outboxRelay", lockAtMostFor = "30s")
    public void publishPending() {
        List<OutboxEvent> events = outboxRepository.findTop100ByPublishedAtIsNullOrderByIdAsc();
        if (!events.isEmpty()) {
            log.debug("Relaying {} outbox events", events.size());
            for (OutboxEvent event : events) {
                Span span = getSpan(event);
                try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
                    publisher.publishFromRelay(
                            event.getTopic(), event.getMessageKey(), event.getPayload(), event.getId());
                    event.setPublishedAt(Instant.now());
                    outboxRepository.save(event);
                } catch (PublishException ex) {
                    span.error(ex);
                    log.warn("PublishException at EventId: {}, topic: {}", event.getId(), event.getTopic());
                    break;
                } finally {
                    span.end();
                }
            }
        }
    }

    private Span getSpan(OutboxEvent event) {
        Span span = null;
        String traceParent = event.getTraceParent();
        if (traceParent != null && !traceParent.isEmpty()) {
            String[] parts = traceParent.split("-");
            if (parts.length ==4) {
                TraceContext parentContext = tracer.traceContextBuilder()
                        .traceId(parts[1])
                        .spanId(parts[2])
                        .sampled(true)
                        .build();
                span = tracer.spanBuilder()
                        .setParent(parentContext)
                        .name("outbox.publish")
                        .start();
            }
        }
        if (span == null) {
            span = tracer.nextSpan().name("outbox.publish").start();
        }
        return span;
    }
}

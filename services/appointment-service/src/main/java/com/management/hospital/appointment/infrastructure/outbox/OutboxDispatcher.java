package com.management.hospital.appointment.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);
    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public OutboxDispatcher(
            OutboxEventRepository outboxEventRepository,
            KafkaTemplate<String, Object> kafkaTemplate,
            ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Immediate low-latency dispatch: triggered immediately after the originating
     * database transaction commits.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOutboxCreated(OutboxCreatedEvent event) {
        log.debug("Immediate outbox dispatch triggered for event {}", event.outboxEventId());
        dispatchById(event.outboxEventId());
    }

    /**
     * Resilient background poller: periodically scans for any PENDING events
     * that failed immediate dispatch or were interrupted during an outage/restart.
     */
    @Scheduled(fixedDelayString = "${outbox.poller.fixed-delay-ms:5000}")
    public void pollPendingOutboxEvents() {
        List<OutboxEvent> pendingEvents = outboxEventRepository
                .findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if (!pendingEvents.isEmpty()) {
            log.info("Outbox poller found {} pending event(s) to process", pendingEvents.size());
            for (OutboxEvent outboxEvent : pendingEvents) {
                dispatch(outboxEvent);
            }
        }
    }

    public void dispatchById(UUID outboxEventId) {
        outboxEventRepository.findByIdAndStatus(outboxEventId, OutboxStatus.PENDING).ifPresent(outboxEvent -> {
            if (outboxEvent.getStatus() == OutboxStatus.PENDING) {
                dispatch(outboxEvent);
            }
        });
    }

    public void dispatch(OutboxEvent outboxEvent) {
        try {
            Class<?> targetClass = Class.forName(outboxEvent.getPayloadClass());
            Object eventObj = objectMapper.readValue(outboxEvent.getPayload(), targetClass);

            kafkaTemplate.send(outboxEvent.getTopic(), outboxEvent.getAggregateId().toString(), eventObj)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            markPublishedInNewTx(outboxEvent.getId());
                            log.info("Successfully published outbox event {} to topic {}",
                                    outboxEvent.getId(), outboxEvent.getTopic());
                        } else {
                            recordFailureInNewTx(outboxEvent.getId(), ex.getMessage());
                            log.warn("Failed to publish outbox event {} to Kafka: {}",
                                    outboxEvent.getId(), ex.getMessage());
                        }
                    });
        } catch (Exception ex) {
            recordFailureInNewTx(outboxEvent.getId(), ex.getMessage());
            log.error("Error preparing outbox event {} for dispatch: {}", outboxEvent.getId(), ex.getMessage(), ex);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markPublishedInNewTx(UUID outboxEventId) {
        outboxEventRepository.findByIdAndStatus(outboxEventId, OutboxStatus.PENDING).ifPresent(event -> {
            event.markPublished();
            outboxEventRepository.save(event);
        });
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailureInNewTx(UUID outboxEventId, String reason) {
        outboxEventRepository.findByIdAndStatus(outboxEventId, OutboxStatus.PENDING).ifPresent(event -> {
            event.recordFailure(MAX_RETRIES);
            outboxEventRepository.save(event);
        });
    }
}

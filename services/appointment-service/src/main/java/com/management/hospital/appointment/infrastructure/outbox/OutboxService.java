package com.management.hospital.appointment.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher applicationEventPublisher;

    public OutboxService(
            OutboxEventRepository outboxEventRepository,
            ObjectMapper objectMapper,
            ApplicationEventPublisher applicationEventPublisher) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    /**
     * Saves the domain event to the outbox table within the caller's active database transaction.
     * Emits an internal application event to trigger immediate dispatch upon transaction commit.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxEvent saveEvent(String aggregateType, UUID aggregateId, String eventType, String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            OutboxEvent outboxEvent = new OutboxEvent(
                    aggregateType,
                    aggregateId,
                    eventType,
                    event.getClass().getName(),
                    topic,
                    payload
            );

            OutboxEvent saved = outboxEventRepository.save(outboxEvent);
            log.info("Saved outbox event {} for aggregate {} [{}]", saved.getId(), aggregateId, aggregateType);

            applicationEventPublisher.publishEvent(new OutboxCreatedEvent(saved.getId()));
            return saved;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize outbox event payload for aggregate " + aggregateId, e);
        }
    }
}

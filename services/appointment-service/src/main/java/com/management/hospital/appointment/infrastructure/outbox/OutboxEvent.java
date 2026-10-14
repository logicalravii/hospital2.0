package com.management.hospital.appointment.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outbox_event")
public class OutboxEvent {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "payload_class", nullable = false, length = 255)
    private String payloadClass;

    @Column(name = "topic", nullable = false, length = 150)
    private String topic;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected OutboxEvent() {
        // Required by JPA
    }

    public OutboxEvent(String aggregateType, UUID aggregateId, String eventType, String payloadClass, String topic, String payload) {
        this.id = UUID.randomUUID();
        this.aggregateType = Objects.requireNonNull(aggregateType, "Aggregate type is required");
        this.aggregateId = Objects.requireNonNull(aggregateId, "Aggregate ID is required");
        this.eventType = Objects.requireNonNull(eventType, "Event type is required");
        this.payloadClass = Objects.requireNonNull(payloadClass, "Payload class is required");
        this.topic = Objects.requireNonNull(topic, "Topic is required");
        this.payload = Objects.requireNonNull(payload, "Payload is required");
        this.status = OutboxStatus.PENDING;
        this.retryCount = 0;
        this.createdAt = Instant.now();
    }

    public void markPublished() {
        this.status = OutboxStatus.PUBLISHED;
        this.processedAt = Instant.now();
    }

    public void recordFailure(int maxRetries) {
        this.retryCount++;
        if (this.retryCount >= maxRetries) {
            this.status = OutboxStatus.FAILED;
        }
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayloadClass() {
        return payloadClass;
    }

    public String getTopic() {
        return topic;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}

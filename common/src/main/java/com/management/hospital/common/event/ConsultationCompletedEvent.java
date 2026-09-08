package com.management.hospital.common.event;

import java.time.Instant;
import java.util.UUID;

public record ConsultationCompletedEvent(
    UUID eventId,
    UUID consultationId,
    UUID appointmentId,
    UUID patientId,
    UUID doctorId,
    String diagnosis,
    Instant occurredAt
) implements DomainEvent {
    public static final String TOPIC = "hospital.consultation.events";
    public static final String EVENT_TYPE = "CONSULTATION_COMPLETED";

    public ConsultationCompletedEvent(UUID consultationId, UUID appointmentId, UUID patientId, UUID doctorId, String diagnosis) {
        this(UUID.randomUUID(), consultationId, appointmentId, patientId, doctorId, diagnosis, Instant.now());
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}

package com.management.hospital.common.event;

import java.time.Instant;
import java.util.UUID;

public record AppointmentCancelledEvent(
        UUID eventId,
        UUID appointmentId,
        UUID patientId,
        UUID doctorId,
        String reason,
        Instant occurredAt) implements DomainEvent {
    public static final String TOPIC = "hospital.appointment.saga.events";
    public static final String EVENT_TYPE = "APPOINTMENT_CANCELLED";

    public AppointmentCancelledEvent(UUID appointmentId, UUID patientId, UUID doctorId, String reason) {

        this(UUID.randomUUID(), appointmentId, patientId, doctorId, reason, Instant.now());
    }

    public String eventType() {
        return EVENT_TYPE;
    }
}

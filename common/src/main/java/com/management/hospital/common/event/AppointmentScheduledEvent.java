package com.management.hospital.common.event;

import java.time.Instant;
import java.util.UUID;

public record AppointmentScheduledEvent(
    UUID eventId,
    UUID appointmentId,
    UUID patientId,
    UUID doctorId,
    Instant startTime,
    Instant endTime,
    String appointmentType,
    Instant occurredAt
) implements DomainEvent {
    public static final String TOPIC = "hospital.appointment.events";
    public static final String EVENT_TYPE = "APPOINTMENT_SCHEDULED";

    public AppointmentScheduledEvent(UUID appointmentId, UUID patientId, UUID doctorId, Instant startTime, Instant endTime, String appointmentType) {
        this(UUID.randomUUID(), appointmentId, patientId, doctorId, startTime, endTime, appointmentType, Instant.now());
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}

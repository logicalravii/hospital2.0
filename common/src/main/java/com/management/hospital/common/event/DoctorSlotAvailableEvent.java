package com.management.hospital.common.event;

import java.time.Instant;
import java.util.UUID;

public record DoctorSlotAvailableEvent(UUID eventId, UUID appointmentId, UUID doctorId, Instant occurredAt)
        implements DomainEvent {
    public static final String TOPIC = "hospital.doctor.events";
    public static final String EVENT_TYPE = "DOCTOR_SLOT_AVAILABLE";

    public DoctorSlotAvailableEvent(UUID appointmentId, UUID doctorId) {
        this(UUID.randomUUID(), appointmentId, doctorId, Instant.now());
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}

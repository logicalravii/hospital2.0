package com.management.hospital.common.event;

import java.time.Instant;
import java.util.UUID;

public record DoctorSlotOverlap(UUID eventId, UUID appointmentId, UUID doctorId, Instant occurredAt)
        implements DomainEvent {
    public static final String TOPIC = "hospital.doctor.overlap";
    public static final String EVENT_TYPE = "DOCTOR_SLOT_OVERLAP";

    public DoctorSlotOverlap(UUID appointmentId, UUID doctorId) {
        this(UUID.randomUUID(), appointmentId, doctorId, Instant.now());
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}

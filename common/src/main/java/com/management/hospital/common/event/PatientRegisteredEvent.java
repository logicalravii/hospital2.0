package com.management.hospital.common.event;

import java.time.Instant;
import java.util.UUID;

public record PatientRegisteredEvent(
    UUID eventId,
    UUID patientId,
    String mrn,
    String firstName,
    String lastName,
    String email,
    String phone,
    Instant occurredAt
) implements DomainEvent {
    public static final String TOPIC = "hospital.patient.events";
    public static final String EVENT_TYPE = "PATIENT_REGISTERED";

    public PatientRegisteredEvent(UUID patientId, String mrn, String firstName, String lastName, String email, String phone) {
        this(UUID.randomUUID(), patientId, mrn, firstName, lastName, email, phone, Instant.now());
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}

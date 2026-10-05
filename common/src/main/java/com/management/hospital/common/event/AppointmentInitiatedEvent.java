package com.management.hospital.common.event;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentInitiatedEvent(UUID eventId,
        UUID appointmentId,
        UUID patientId,
        UUID doctorId,
        LocalTime startTime,
        LocalTime endTime,
        LocalDate appointmentDate,
        String appointmentType,
        Instant occurredAt) implements DomainEvent {
    public static final String TOPIC = "hospital.appointment.initiated";
    public static final String EVENT_TYPE = "APPOINTMENT_INITIATED";

    public AppointmentInitiatedEvent(UUID appointmentId, UUID patientId, UUID doctorId, LocalTime startTime,
            LocalTime endTime, LocalDate appointmentDate, String appointmentType) {
        this(UUID.randomUUID(), appointmentId, patientId, doctorId, startTime, endTime, appointmentDate,
                appointmentType, Instant.now());
    }

    @Override
    public String eventType() {
        return EVENT_TYPE;
    }
}

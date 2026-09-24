package com.management.hospital.appointment.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum AppointmentStatus {
    PENDING_VALIDATION,
    REJECTED,
    SCHEDULED,
    CONFIRMED,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED;

    @JsonCreator
    public static AppointmentStatus fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Appointment status cannot be null or blank");
        }
        String trimmed = value.trim();
        for (AppointmentStatus s : values()) {
            if (s.name().equalsIgnoreCase(trimmed)) {
                return s;
            }
        }
        throw new IllegalArgumentException(
                "Invalid appointment status '%s'. Allowed statuses are: %s".formatted(trimmed,
                        Arrays.toString(values())));
    }
}

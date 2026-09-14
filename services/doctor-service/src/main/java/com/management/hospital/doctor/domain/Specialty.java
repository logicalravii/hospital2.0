package com.management.hospital.doctor.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum Specialty {
    CARDIOLOGY,
    DERMATOLOGY,
    NEUROLOGY,
    ORTHOPEDICS,
    PEDIATRICS,
    GENERAL_MEDICINE,
    GYNECOLOGY,
    ONCOLOGY,
    OPHTHALMOLOGY,
    ENT,
    PSYCHIATRY,
    SURGERY;

    @JsonCreator
    public static Specialty fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Specialty cannot be null or blank");
        }
        String trimmed = value.trim();
        for (Specialty s : values()) {
            if (s.name().equalsIgnoreCase(trimmed)) {
                return s;
            }
        }
        throw new IllegalArgumentException(
            "Invalid specialty '%s'. Allowed specialties are: %s".formatted(trimmed, Arrays.toString(values()))
        );
    }
}

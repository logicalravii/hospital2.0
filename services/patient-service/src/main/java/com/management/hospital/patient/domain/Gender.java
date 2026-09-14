package com.management.hospital.patient.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum Gender {
    MALE,
    FEMALE,
    OTHER,
    UNKNOWN;

    @JsonCreator
    public static Gender fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Gender cannot be null or blank");
        }
        String trimmed = value.trim();
        for (Gender gender : values()) {
            if (gender.name().equalsIgnoreCase(trimmed)) {
                return gender;
            }
        }
        throw new IllegalArgumentException(
            "Invalid gender '%s'. Allowed values are: %s".formatted(trimmed, Arrays.toString(values()))
        );
    }
}


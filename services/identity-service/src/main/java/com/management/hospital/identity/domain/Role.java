package com.management.hospital.identity.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Arrays;

public enum Role {
    ADMIN,
    DOCTOR,
    NURSE,
    RECEPTIONIST,
    PHARMACIST,
    LAB_TECHNICIAN,
    BILLING_STAFF;

    public String toAuthority() {
        return "ROLE_" + name();
    }

    @JsonCreator
    public static Role fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Role cannot be null or blank");
        }
        String trimmed = value.trim();
        for (Role role : values()) {
            if (role.name().equalsIgnoreCase(trimmed)) {
                return role;
            }
        }
        throw new IllegalArgumentException(
            "Invalid role '%s'. Allowed roles are: %s".formatted(trimmed, Arrays.toString(values()))
        );
    }
}


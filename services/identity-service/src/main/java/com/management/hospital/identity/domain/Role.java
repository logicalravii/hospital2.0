package com.management.hospital.identity.domain;

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
}

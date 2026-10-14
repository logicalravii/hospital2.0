package com.management.hospital.appointment.infrastructure.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}

package com.management.hospital.appointment.infrastructure.outbox;

import java.util.UUID;

public record OutboxCreatedEvent(UUID outboxEventId) {
}

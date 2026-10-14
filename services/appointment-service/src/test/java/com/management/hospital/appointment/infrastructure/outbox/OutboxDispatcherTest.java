package com.management.hospital.appointment.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.management.hospital.common.event.AppointmentInitiatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxDispatcherTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private ObjectMapper objectMapper;
    private OutboxDispatcher outboxDispatcher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        outboxDispatcher = new OutboxDispatcher(outboxEventRepository, kafkaTemplate, objectMapper);
    }

    @Test
    @DisplayName("Should dispatch event and mark published when Kafka send succeeds")
    void shouldDispatchAndMarkPublished() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        AppointmentInitiatedEvent domainEvent = new AppointmentInitiatedEvent(
                appointmentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                LocalDate.now().plusDays(2),
                "GENERAL_CONSULTATION"
        );

        String jsonPayload = objectMapper.writeValueAsString(domainEvent);
        OutboxEvent outboxEvent = new OutboxEvent(
                "Appointment",
                appointmentId,
                domainEvent.eventType(),
                domainEvent.getClass().getName(),
                AppointmentInitiatedEvent.TOPIC,
                jsonPayload
        );

        when(outboxEventRepository.findById(outboxEvent.getId())).thenReturn(Optional.of(outboxEvent));
        when(kafkaTemplate.send(eq(AppointmentInitiatedEvent.TOPIC), eq(appointmentId.toString()), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxDispatcher.onOutboxCreated(new OutboxCreatedEvent(outboxEvent.getId()));

        verify(kafkaTemplate).send(eq(AppointmentInitiatedEvent.TOPIC), eq(appointmentId.toString()), any(AppointmentInitiatedEvent.class));
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(outboxEvent.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should poll pending outbox events and dispatch them")
    void shouldPollPendingEvents() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        AppointmentInitiatedEvent domainEvent = new AppointmentInitiatedEvent(
                appointmentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalTime.of(14, 0),
                LocalTime.of(14, 30),
                LocalDate.now().plusDays(3),
                "GENERAL_CONSULTATION"
        );

        String jsonPayload = objectMapper.writeValueAsString(domainEvent);
        OutboxEvent outboxEvent = new OutboxEvent(
                "Appointment",
                appointmentId,
                domainEvent.eventType(),
                domainEvent.getClass().getName(),
                AppointmentInitiatedEvent.TOPIC,
                jsonPayload
        );

        when(outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
                .thenReturn(List.of(outboxEvent));
        when(outboxEventRepository.findById(outboxEvent.getId())).thenReturn(Optional.of(outboxEvent));
        when(kafkaTemplate.send(eq(AppointmentInitiatedEvent.TOPIC), eq(appointmentId.toString()), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxDispatcher.pollPendingOutboxEvents();

        verify(kafkaTemplate).send(eq(AppointmentInitiatedEvent.TOPIC), eq(appointmentId.toString()), any(AppointmentInitiatedEvent.class));
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Should record failure and increment retry count when Kafka send fails")
    void shouldRecordFailureWhenKafkaFails() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        AppointmentInitiatedEvent domainEvent = new AppointmentInitiatedEvent(
                appointmentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                LocalTime.of(10, 0),
                LocalTime.of(10, 30),
                LocalDate.now().plusDays(2),
                "GENERAL_CONSULTATION"
        );

        String jsonPayload = objectMapper.writeValueAsString(domainEvent);
        OutboxEvent outboxEvent = new OutboxEvent(
                "Appointment",
                appointmentId,
                domainEvent.eventType(),
                domainEvent.getClass().getName(),
                AppointmentInitiatedEvent.TOPIC,
                jsonPayload
        );

        when(outboxEventRepository.findById(outboxEvent.getId())).thenReturn(Optional.of(outboxEvent));
        when(kafkaTemplate.send(eq(AppointmentInitiatedEvent.TOPIC), eq(appointmentId.toString()), any()))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Kafka timeout")));
        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        outboxDispatcher.onOutboxCreated(new OutboxCreatedEvent(outboxEvent.getId()));

        assertThat(outboxEvent.getRetryCount()).isEqualTo(1);
        assertThat(outboxEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }
}

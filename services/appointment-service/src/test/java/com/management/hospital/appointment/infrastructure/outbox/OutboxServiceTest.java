package com.management.hospital.appointment.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.management.hospital.common.event.AppointmentInitiatedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private ObjectMapper objectMapper;
    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        outboxService = new OutboxService(outboxEventRepository, objectMapper, applicationEventPublisher);
    }

    @Test
    @DisplayName("Should save OutboxEvent with JSON payload and publish internal OutboxCreatedEvent")
    void shouldSaveEventSuccessfully() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        AppointmentInitiatedEvent event = new AppointmentInitiatedEvent(
                appointmentId,
                patientId,
                doctorId,
                LocalTime.of(9, 0),
                LocalTime.of(9, 30),
                LocalDate.now().plusDays(1),
                "GENERAL_CONSULTATION"
        );

        when(outboxEventRepository.save(any(OutboxEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OutboxEvent saved = outboxService.saveEvent(
                "Appointment",
                appointmentId,
                event.eventType(),
                AppointmentInitiatedEvent.TOPIC,
                event
        );

        assertThat(saved.getAggregateType()).isEqualTo("Appointment");
        assertThat(saved.getAggregateId()).isEqualTo(appointmentId);
        assertThat(saved.getEventType()).isEqualTo("APPOINTMENT_INITIATED");
        assertThat(saved.getTopic()).isEqualTo(AppointmentInitiatedEvent.TOPIC);
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(saved.getPayload()).contains(appointmentId.toString());

        ArgumentCaptor<OutboxCreatedEvent> internalEventCaptor = ArgumentCaptor.forClass(OutboxCreatedEvent.class);
        verify(applicationEventPublisher).publishEvent(internalEventCaptor.capture());
        assertThat(internalEventCaptor.getValue().outboxEventId()).isEqualTo(saved.getId());
    }
}

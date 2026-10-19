package com.management.hospital.consultation.infrastructure;

import com.management.hospital.common.event.AppointmentCancelledEvent;
import com.management.hospital.common.event.AppointmentInitiatedEvent;
import com.management.hospital.common.event.AppointmentScheduledEvent;
import com.management.hospital.consultation.domain.AppointmentProjection;
import com.management.hospital.consultation.domain.AppointmentProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentConsumerTest {

    @Mock
    private AppointmentProjectionRepository projectionRepository;

    private AppointmentInitiatedConsumer initiatedConsumer;
    private AppointmentSagaConsumer sagaConsumer;

    @BeforeEach
    void setUp() {
        initiatedConsumer = new AppointmentInitiatedConsumer(projectionRepository);
        sagaConsumer = new AppointmentSagaConsumer(projectionRepository);
    }

    @Test
    @DisplayName("Should project AppointmentInitiatedEvent as PENDING_VALIDATION")
    void shouldProjectInitiatedEvent() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        AppointmentInitiatedEvent event = new AppointmentInitiatedEvent(
                appointmentId, patientId, doctorId,
                LocalTime.of(10, 0), LocalTime.of(10, 30),
                LocalDate.now().plusDays(1), "GENERAL"
        );

        when(projectionRepository.findById(appointmentId)).thenReturn(Optional.empty());

        initiatedConsumer.handleAppointmentInitiated(event);

        ArgumentCaptor<AppointmentProjection> captor = ArgumentCaptor.forClass(AppointmentProjection.class);
        verify(projectionRepository).save(captor.capture());

        AppointmentProjection captured = captor.getValue();
        assertThat(captured.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(captured.getPatientId()).isEqualTo(patientId);
        assertThat(captured.getDoctorId()).isEqualTo(doctorId);
        assertThat(captured.getStatus()).isEqualTo("PENDING_VALIDATION");
    }

    @Test
    @DisplayName("Should project AppointmentScheduledEvent as CONFIRMED")
    void shouldProjectScheduledEvent() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        AppointmentScheduledEvent event = new AppointmentScheduledEvent(
                appointmentId, patientId, doctorId,
                Instant.now(), Instant.now().plusSeconds(1800), "GENERAL"
        );

        AppointmentProjection existing = new AppointmentProjection(appointmentId, patientId, doctorId, "PENDING_VALIDATION");
        when(projectionRepository.findById(appointmentId)).thenReturn(Optional.of(existing));

        sagaConsumer.handleScheduled(event);

        verify(projectionRepository).save(existing);
        assertThat(existing.getStatus()).isEqualTo("CONFIRMED");
    }

    @Test
    @DisplayName("Should project AppointmentCancelledEvent as CANCELLED")
    void shouldProjectCancelledEvent() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
                appointmentId, patientId, doctorId, "Patient request"
        );

        AppointmentProjection existing = new AppointmentProjection(appointmentId, patientId, doctorId, "CONFIRMED");
        when(projectionRepository.findById(appointmentId)).thenReturn(Optional.of(existing));

        sagaConsumer.handleCancelled(event);

        verify(projectionRepository).save(existing);
        assertThat(existing.getStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("Should handle unknown object without throwing")
    void shouldHandleUnknownObject() {
        sagaConsumer.handleUnknown("Some unrecognized message payload");
    }
}

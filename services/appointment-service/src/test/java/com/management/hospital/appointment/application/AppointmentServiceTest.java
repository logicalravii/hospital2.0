package com.management.hospital.appointment.application;

import com.management.hospital.appointment.api.dto.AppointmentResponse;
import com.management.hospital.appointment.api.dto.BookAppointmentRequest;
import com.management.hospital.appointment.api.dto.CancelAppointmentRequest;
import com.management.hospital.appointment.domain.Appointment;
import com.management.hospital.appointment.domain.AppointmentStatus;
import com.management.hospital.appointment.infrastructure.AppointmentEventPublisher;
import com.management.hospital.appointment.infrastructure.AppointmentRepository;
import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.common.event.AppointmentCancelledEvent;
import com.management.hospital.common.event.AppointmentInitiatedEvent;
import com.management.hospital.appointment.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    @Mock
    private AppointmentRepository appointmentRepository;

    @Mock
    private AppointmentEventPublisher eventPublisher;

    @Mock
    private OutboxService outboxService;

    private AppointmentService appointmentService;

    @BeforeEach
    void setUp() {
        appointmentService = new AppointmentService(appointmentRepository, eventPublisher, outboxService);
    }

    @Test
    @DisplayName("Should successfully book appointment in PENDING_VALIDATION status and save event to Transactional Outbox")
    void shouldBookAppointment() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(10, 30);

        BookAppointmentRequest request = new BookAppointmentRequest(
                patientId, doctorId, date, start, end, "General consultation", "GENERAL_CONSULTATION"
        );

        when(appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusNot(doctorId, date, AppointmentStatus.CANCELLED))
                .thenReturn(List.of());
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.bookAppointment(request);

        assertThat(response.patientId()).isEqualTo(patientId);
        assertThat(response.doctorId()).isEqualTo(doctorId);
        assertThat(response.status()).isEqualTo(AppointmentStatus.PENDING_VALIDATION);

        verify(appointmentRepository).save(any(Appointment.class));

        ArgumentCaptor<AppointmentInitiatedEvent> eventCaptor = ArgumentCaptor.forClass(AppointmentInitiatedEvent.class);
        verify(outboxService).saveEvent(
                eq("Appointment"),
                any(UUID.class),
                eq("APPOINTMENT_INITIATED"),
                eq(AppointmentInitiatedEvent.TOPIC),
                eventCaptor.capture()
        );
        AppointmentInitiatedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.patientId()).isEqualTo(patientId);
        assertThat(publishedEvent.doctorId()).isEqualTo(doctorId);
        assertThat(publishedEvent.appointmentDate()).isEqualTo(date);
        assertThat(publishedEvent.startTime()).isEqualTo(start);
        assertThat(publishedEvent.endTime()).isEqualTo(end);
    }

    @Test
    @DisplayName("Should reject booking when doctor has overlapping appointment")
    void shouldRejectOverlappingBooking() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(11, 0);

        Appointment existing = new Appointment(
                UUID.randomUUID(), doctorId, date, LocalTime.of(9, 30), LocalTime.of(10, 30), "Existing"
        );

        when(appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusNot(doctorId, date, AppointmentStatus.CANCELLED))
                .thenReturn(List.of(existing));

        BookAppointmentRequest request = new BookAppointmentRequest(
                patientId, doctorId, date, start, end, "Conflict test", null
        );

        assertThatThrownBy(() -> appointmentService.bookAppointment(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Doctor already has an active appointment");
    }

    @Test
    @DisplayName("Should cancel appointment and emit cancellation event")
    void shouldCancelAppointment() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = new Appointment(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(9, 30), "Consultation"
        );

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelAppointmentRequest cancelRequest = new CancelAppointmentRequest("Patient cannot attend");
        AppointmentResponse response = appointmentService.cancelAppointment(appointmentId, cancelRequest);

        assertThat(response.status()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(response.cancellationReason()).isEqualTo("Patient cannot attend");

        ArgumentCaptor<AppointmentCancelledEvent> eventCaptor = ArgumentCaptor.forClass(AppointmentCancelledEvent.class);
        verify(eventPublisher).publishAppointmentCancelled(eventCaptor.capture());
        assertThat(eventCaptor.getValue().reason()).isEqualTo("Patient cannot attend");
    }

    @Test
    @DisplayName("Should confirm appointment successfully")
    void shouldConfirmAppointment() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = new Appointment(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(9, 30), "Consultation", AppointmentStatus.PENDING_VALIDATION
        );

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.confirmAppointment(appointmentId);
        assertThat(response.status()).isEqualTo(AppointmentStatus.CONFIRMED);
        verify(appointmentRepository).save(appointment);
        verify(eventPublisher).publishAppointmentScheduled(any());
    }

    @Test
    @DisplayName("Should start appointment successfully")
    void shouldStartAppointment() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = new Appointment(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(9, 30), "Consultation", AppointmentStatus.SCHEDULED
        );
        appointment.confirm();

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.startAppointment(appointmentId);
        assertThat(response.status()).isEqualTo(AppointmentStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should complete appointment successfully")
    void shouldCompleteAppointment() {
        UUID appointmentId = UUID.randomUUID();
        Appointment appointment = new Appointment(
                UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1),
                LocalTime.of(9, 0), LocalTime.of(9, 30), "Consultation", AppointmentStatus.SCHEDULED
        );
        appointment.confirm();
        appointment.start();

        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.of(appointment));
        when(appointmentRepository.save(any(Appointment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppointmentResponse response = appointmentService.completeAppointment(appointmentId);
        assertThat(response.status()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when appointment does not exist")
    void shouldThrowNotFoundWhenAppointmentNotFound() {
        UUID appointmentId = UUID.randomUUID();
        when(appointmentRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appointmentService.getAppointmentById(appointmentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Appointment");
    }
}

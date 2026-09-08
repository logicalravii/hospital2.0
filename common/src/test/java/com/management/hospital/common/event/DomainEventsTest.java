package com.management.hospital.common.event;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DomainEventsTest {

    @Test
    @DisplayName("PatientRegisteredEvent should populate default eventId, timestamp and topic")
    void shouldCreatePatientRegisteredEvent() {
        UUID patientId = UUID.randomUUID();
        PatientRegisteredEvent event = new PatientRegisteredEvent(
            patientId,
            "MRN-2026-0001",
            "John",
            "Doe",
            "john.doe@example.com",
            "+1234567890"
        );

        assertThat(event.eventId()).isNotNull();
        assertThat(event.patientId()).isEqualTo(patientId);
        assertThat(event.mrn()).isEqualTo("MRN-2026-0001");
        assertThat(event.eventType()).isEqualTo("PATIENT_REGISTERED");
        assertThat(event.occurredAt()).isNotNull();
        assertThat(PatientRegisteredEvent.TOPIC).isEqualTo("hospital.patient.events");
    }

    @Test
    @DisplayName("AppointmentScheduledEvent should capture doctor and slot time range")
    void shouldCreateAppointmentScheduledEvent() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Instant startTime = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant endTime = startTime.plus(30, ChronoUnit.MINUTES);

        AppointmentScheduledEvent event = new AppointmentScheduledEvent(
            appointmentId,
            patientId,
            doctorId,
            startTime,
            endTime,
            "GENERAL_CHECKUP"
        );

        assertThat(event.appointmentId()).isEqualTo(appointmentId);
        assertThat(event.patientId()).isEqualTo(patientId);
        assertThat(event.doctorId()).isEqualTo(doctorId);
        assertThat(event.appointmentType()).isEqualTo("GENERAL_CHECKUP");
        assertThat(event.eventType()).isEqualTo("APPOINTMENT_SCHEDULED");
        assertThat(AppointmentScheduledEvent.TOPIC).isEqualTo("hospital.appointment.events");
    }

    @Test
    @DisplayName("AppointmentCancelledEvent should capture cancellation reason")
    void shouldCreateAppointmentCancelledEvent() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
            appointmentId,
            patientId,
            doctorId,
            "Patient emergency"
        );

        assertThat(event.appointmentId()).isEqualTo(appointmentId);
        assertThat(event.reason()).isEqualTo("Patient emergency");
        assertThat(event.eventType()).isEqualTo("APPOINTMENT_CANCELLED");
    }

    @Test
    @DisplayName("ConsultationCompletedEvent should capture diagnosis and encounter links")
    void shouldCreateConsultationCompletedEvent() {
        UUID consultationId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        ConsultationCompletedEvent event = new ConsultationCompletedEvent(
            consultationId,
            appointmentId,
            patientId,
            doctorId,
            "Acute bronchitis"
        );

        assertThat(event.consultationId()).isEqualTo(consultationId);
        assertThat(event.appointmentId()).isEqualTo(appointmentId);
        assertThat(event.diagnosis()).isEqualTo("Acute bronchitis");
        assertThat(event.eventType()).isEqualTo("CONSULTATION_COMPLETED");
        assertThat(ConsultationCompletedEvent.TOPIC).isEqualTo("hospital.consultation.events");
    }
}

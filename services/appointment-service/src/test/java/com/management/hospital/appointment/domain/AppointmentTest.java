package com.management.hospital.appointment.domain;

import com.management.hospital.common.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AppointmentTest {

    @Test
    @DisplayName("Should create valid appointment with SCHEDULED status")
    void shouldCreateValidAppointment() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(10, 30);

        Appointment appointment = new Appointment(patientId, doctorId, date, start, end, "Routine checkup");

        assertThat(appointment.getId()).isNotNull();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.SCHEDULED);
        assertThat(appointment.getPatientId()).isEqualTo(patientId);
        assertThat(appointment.getDoctorId()).isEqualTo(doctorId);
        assertThat(appointment.getAppointmentDate()).isEqualTo(date);
        assertThat(appointment.getReason()).isEqualTo("Routine checkup");
    }

    @Test
    @DisplayName("Should reject end time before or equal to start time")
    void shouldRejectInvalidTimes() {
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(1);

        assertThatThrownBy(() -> new Appointment(patientId, doctorId, date, LocalTime.of(11, 0), LocalTime.of(10, 0), "Reason"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must be after start time");
    }

    @Test
    @DisplayName("Should follow lifecycle: SCHEDULED -> CONFIRMED -> IN_PROGRESS -> COMPLETED")
    void shouldFollowValidLifecycle() {
        Appointment appointment = new Appointment(
            UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1),
            LocalTime.of(9, 0), LocalTime.of(9, 30), "Consultation"
        );

        appointment.confirm();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CONFIRMED);

        appointment.start();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.IN_PROGRESS);

        appointment.complete();
        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.COMPLETED);
    }

    @Test
    @DisplayName("Should cancel scheduled appointment with reason")
    void shouldCancelAppointment() {
        Appointment appointment = new Appointment(
            UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1),
            LocalTime.of(9, 0), LocalTime.of(9, 30), "Consultation"
        );

        appointment.cancel("Patient requested cancellation");

        assertThat(appointment.getStatus()).isEqualTo(AppointmentStatus.CANCELLED);
        assertThat(appointment.getCancellationReason()).isEqualTo("Patient requested cancellation");
    }

    @Test
    @DisplayName("Should reject cancellation on COMPLETED appointment")
    void shouldRejectCancellationOnCompleted() {
        Appointment appointment = new Appointment(
            UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1),
            LocalTime.of(9, 0), LocalTime.of(9, 30), "Consultation"
        );

        appointment.complete();

        assertThatThrownBy(() -> appointment.cancel("Too late"))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Cannot cancel an appointment that has already been completed");
    }

    @Test
    @DisplayName("Should detect overlapping appointment time range")
    void shouldDetectOverlap() {
        LocalDate date = LocalDate.now().plusDays(2);
        Appointment appointment = new Appointment(
            UUID.randomUUID(), UUID.randomUUID(), date,
            LocalTime.of(10, 0), LocalTime.of(11, 0), "Consultation"
        );

        // Overlaps inside
        assertThat(appointment.overlapsWith(date, LocalTime.of(10, 15), LocalTime.of(10, 45))).isTrue();
        // Contiguous after - does NOT overlap
        assertThat(appointment.overlapsWith(date, LocalTime.of(11, 0), LocalTime.of(11, 30))).isFalse();
        // Different date - does NOT overlap
        assertThat(appointment.overlapsWith(date.plusDays(1), LocalTime.of(10, 0), LocalTime.of(11, 0))).isFalse();
    }
}

package com.management.hospital.appointment.api.dto;

import com.management.hospital.appointment.domain.Appointment;
import com.management.hospital.appointment.domain.AppointmentStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID patientId,
    UUID doctorId,
    LocalDate appointmentDate,
    LocalTime startTime,
    LocalTime endTime,
    AppointmentStatus status,
    String reason,
    String cancellationReason,
    Instant createdAt,
    Instant updatedAt
) {
    public static AppointmentResponse from(Appointment appointment) {
        return new AppointmentResponse(
            appointment.getId(),
            appointment.getPatientId(),
            appointment.getDoctorId(),
            appointment.getAppointmentDate(),
            appointment.getStartTime(),
            appointment.getEndTime(),
            appointment.getStatus(),
            appointment.getReason(),
            appointment.getCancellationReason(),
            appointment.getCreatedAt(),
            appointment.getUpdatedAt()
        );
    }
}

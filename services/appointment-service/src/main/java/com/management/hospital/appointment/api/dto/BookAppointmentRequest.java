package com.management.hospital.appointment.api.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record BookAppointmentRequest(
    @NotNull(message = "Patient ID is required")
    UUID patientId,

    @NotNull(message = "Doctor ID is required")
    UUID doctorId,

    @NotNull(message = "Appointment date is required")
    @FutureOrPresent(message = "Appointment date must be today or in the future")
    LocalDate appointmentDate,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "End time is required")
    LocalTime endTime,

    String reason,

    String appointmentType
) {}

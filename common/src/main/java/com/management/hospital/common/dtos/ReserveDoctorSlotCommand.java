package com.management.hospital.common.dtos;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record ReserveDoctorSlotCommand(
        UUID appointmentId,
        UUID doctorId,
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime) {
}
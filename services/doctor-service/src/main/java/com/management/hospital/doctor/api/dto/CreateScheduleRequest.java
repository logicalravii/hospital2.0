package com.management.hospital.doctor.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record CreateScheduleRequest(
    @NotNull(message = "Day of week is required")
    DayOfWeek dayOfWeek,

    @NotNull(message = "Start time is required")
    LocalTime startTime,

    @NotNull(message = "End time is required")
    LocalTime endTime,

    @Min(value = 5, message = "Slot duration must be at least 5 minutes")
    int slotDurationMinutes,

    @Min(value = 1, message = "Max patients must be at least 1")
    int maxPatients
) {}

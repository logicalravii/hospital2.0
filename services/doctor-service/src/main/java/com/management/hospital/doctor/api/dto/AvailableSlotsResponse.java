package com.management.hospital.doctor.api.dto;

import com.management.hospital.doctor.domain.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record AvailableSlotsResponse(
    UUID doctorId,
    LocalDate date,
    DayOfWeek dayOfWeek,
    List<TimeSlot> slots
) {}

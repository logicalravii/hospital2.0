package com.management.hospital.doctor.domain;

import java.time.LocalTime;

public record TimeSlot(
    LocalTime startTime,
    LocalTime endTime
) {}

package com.management.hospital.doctor.api.dto;

import com.management.hospital.doctor.domain.DoctorSchedule;
import com.management.hospital.doctor.domain.TimeSlot;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record DoctorScheduleResponse(
    UUID id,
    UUID doctorId,
    DayOfWeek dayOfWeek,
    LocalTime startTime,
    LocalTime endTime,
    int slotDurationMinutes,
    int maxPatients,
    boolean active,
    List<TimeSlot> availableSlots
) {
    public static DoctorScheduleResponse from(DoctorSchedule schedule) {
        return new DoctorScheduleResponse(
            schedule.getId(),
            schedule.getDoctor().getId(),
            schedule.getDayOfWeek(),
            schedule.getStartTime(),
            schedule.getEndTime(),
            schedule.getSlotDurationMinutes(),
            schedule.getMaxPatients(),
            schedule.isActive(),
            schedule.generateSlots()
        );
    }
}

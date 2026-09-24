package com.management.hospital.doctor.infrastructure;

import com.management.hospital.doctor.domain.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface DoctorScheduleRepository extends JpaRepository<DoctorSchedule, UUID> {
    List<DoctorSchedule> findByDoctorIdAndActiveTrue(UUID doctorId);

    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndActiveTrue(UUID doctorId, DayOfWeek dayOfWeek);

    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndStartTimeAndEndTimeAndActiveTrue(UUID doctorId,
            DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime);
}

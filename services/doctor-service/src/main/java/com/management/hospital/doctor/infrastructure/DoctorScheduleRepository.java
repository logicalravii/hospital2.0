package com.management.hospital.doctor.infrastructure;

import com.management.hospital.doctor.domain.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    // select * from doctor d
    // inner join doctor_schedule ds
    // on d.id = ds.doctor_id
    // where ds.active='true' and
    // ds.day_of_week='MONDAY' and
    // ds.start_time > '09:00:00' and ds.end_time < '17:00:00';

    @Query("""
            SELECT CASE WHEN COUNT(d) > 0 THEN true ELSE false END FROM DoctorSchedule d
            WHERE d.doctor.id = :doctorId
            AND d.active = true
            AND d.dayOfWeek = :dayOfWeek
            AND d.startTime <= :startTime
            AND d.endTime >= :endTime
            """)
    Boolean isDoctorHasActiveSchedule(
            UUID doctorId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime);

    // List<DoctorSchedule>
    // findByDoctorIdAndDayOfWeekAndActiveTrueAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(UUID
    // doctorId, DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime);
}

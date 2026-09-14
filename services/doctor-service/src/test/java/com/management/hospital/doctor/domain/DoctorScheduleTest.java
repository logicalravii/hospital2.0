package com.management.hospital.doctor.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoctorScheduleTest {

    private Doctor doctor;

    @BeforeEach
    void setUp() {
        doctor = new Doctor(
            null,
            "MED-55555",
            "Allison",
            "Cameron",
            Specialty.CARDIOLOGY,
            "cameron@princeton.edu",
            "+16095551111",
            new BigDecimal("180.00"),
            "Room 202"
        );
    }

    @Test
    @DisplayName("Should generate correct slots based on window and duration")
    void shouldGenerateDiscreteSlots() {
        DoctorSchedule schedule = new DoctorSchedule(
            doctor,
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(11, 0),
            30,
            10
        );

        List<TimeSlot> slots = schedule.generateSlots();

        assertThat(slots).hasSize(4);
        assertThat(slots.get(0)).isEqualTo(new TimeSlot(LocalTime.of(9, 0), LocalTime.of(9, 30)));
        assertThat(slots.get(1)).isEqualTo(new TimeSlot(LocalTime.of(9, 30), LocalTime.of(10, 0)));
        assertThat(slots.get(2)).isEqualTo(new TimeSlot(LocalTime.of(10, 0), LocalTime.of(10, 30)));
        assertThat(slots.get(3)).isEqualTo(new TimeSlot(LocalTime.of(10, 30), LocalTime.of(11, 0)));
    }

    @Test
    @DisplayName("Should reject end time before or equal to start time")
    void shouldRejectInvalidTimeRange() {
        assertThatThrownBy(() -> new DoctorSchedule(
            doctor,
            DayOfWeek.TUESDAY,
            LocalTime.of(14, 0),
            LocalTime.of(13, 0),
            30,
            5
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("must be after start time");
    }

    @Test
    @DisplayName("Should detect schedule overlap on same day")
    void shouldDetectOverlap() {
        DoctorSchedule schedule = new DoctorSchedule(
            doctor,
            DayOfWeek.WEDNESDAY,
            LocalTime.of(9, 0),
            LocalTime.of(12, 0),
            30,
            6
        );

        // Case 1: Partial overlap inside
        assertThat(schedule.overlapsWith(DayOfWeek.WEDNESDAY, LocalTime.of(10, 0), LocalTime.of(11, 0))).isTrue();

        // Case 2: Partial overlap boundary
        assertThat(schedule.overlapsWith(DayOfWeek.WEDNESDAY, LocalTime.of(8, 30), LocalTime.of(9, 30))).isTrue();

        // Case 3: Contiguous start - NO overlap (ends at 9:00, next starts at 9:00)
        assertThat(schedule.overlapsWith(DayOfWeek.WEDNESDAY, LocalTime.of(8, 0), LocalTime.of(9, 0))).isFalse();

        // Case 4: Contiguous end - NO overlap (starts at 12:00)
        assertThat(schedule.overlapsWith(DayOfWeek.WEDNESDAY, LocalTime.of(12, 0), LocalTime.of(14, 0))).isFalse();

        // Case 5: Different day of week - NO overlap
        assertThat(schedule.overlapsWith(DayOfWeek.THURSDAY, LocalTime.of(10, 0), LocalTime.of(11, 0))).isFalse();
    }
}

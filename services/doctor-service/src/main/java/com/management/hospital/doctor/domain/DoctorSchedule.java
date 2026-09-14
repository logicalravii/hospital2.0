package com.management.hospital.doctor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "doctor_schedule")
public class DoctorSchedule {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 20)
    private DayOfWeek dayOfWeek;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "slot_duration_minutes", nullable = false)
    private int slotDurationMinutes;

    @Column(name = "max_patients", nullable = false)
    private int maxPatients;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DoctorSchedule() {
        // Required by JPA
    }

    public DoctorSchedule(
        Doctor doctor,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotDurationMinutes,
        int maxPatients
    ) {
        this.id = UUID.randomUUID();
        this.doctor = Objects.requireNonNull(doctor, "Doctor is required");
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek, "Day of week is required");
        this.startTime = Objects.requireNonNull(startTime, "Start time is required");
        this.endTime = Objects.requireNonNull(endTime, "End time is required");

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("End time (%s) must be after start time (%s)".formatted(endTime, startTime));
        }
        if (slotDurationMinutes <= 0) {
            throw new IllegalArgumentException("Slot duration must be greater than 0 minutes");
        }
        if (maxPatients <= 0) {
            throw new IllegalArgumentException("Max patients must be greater than 0");
        }

        this.slotDurationMinutes = slotDurationMinutes;
        this.maxPatients = maxPatients;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public boolean overlapsWith(DayOfWeek checkDay, LocalTime checkStart, LocalTime checkEnd) {
        if (this.dayOfWeek != checkDay || !this.active) {
            return false;
        }
        // Two intervals [s1, e1) and [s2, e2) overlap if s1 < e2 && s2 < e1
        return this.startTime.isBefore(checkEnd) && checkStart.isBefore(this.endTime);
    }

    public List<TimeSlot> generateSlots() {
        List<TimeSlot> slots = new ArrayList<>();
        LocalTime current = this.startTime;
        while (!current.plusMinutes(slotDurationMinutes).isAfter(this.endTime) && slots.size() < this.maxPatients) {
            LocalTime next = current.plusMinutes(slotDurationMinutes);
            slots.add(new TimeSlot(current, next));
            current = next;
        }
        return slots;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public int getSlotDurationMinutes() {
        return slotDurationMinutes;
    }

    public int getMaxPatients() {
        return maxPatients;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

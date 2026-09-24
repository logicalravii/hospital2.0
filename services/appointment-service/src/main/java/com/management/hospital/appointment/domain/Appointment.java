package com.management.hospital.appointment.domain;

import com.management.hospital.common.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "appointment")
public class Appointment {

    @Id
    private UUID id;

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false)
    private UUID doctorId;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private AppointmentStatus status;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "cancellation_reason", length = 255)
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Appointment() {
        // Required by JPA
    }

    public Appointment(
            UUID patientId,
            UUID doctorId,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            String reason) {
        this(patientId, doctorId, appointmentDate, startTime, endTime, reason, AppointmentStatus.SCHEDULED);
    }

    public Appointment(
            UUID patientId,
            UUID doctorId,
            LocalDate appointmentDate,
            LocalTime startTime,
            LocalTime endTime,
            String reason,
            AppointmentStatus status) {
        this.id = UUID.randomUUID();
        this.patientId = Objects.requireNonNull(patientId, "Patient ID is required");
        this.doctorId = Objects.requireNonNull(doctorId, "Doctor ID is required");
        this.appointmentDate = Objects.requireNonNull(appointmentDate, "Appointment date is required");
        this.startTime = Objects.requireNonNull(startTime, "Start time is required");
        this.endTime = Objects.requireNonNull(endTime, "End time is required");

        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException(
                    "End time (%s) must be after start time (%s)".formatted(endTime, startTime));
        }

        this.status = Objects.requireNonNull(status, "Status is required");
        this.reason = reason != null ? reason.trim() : null;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void confirm() {
        if (this.status != AppointmentStatus.SCHEDULED && this.status != AppointmentStatus.PENDING_VALIDATION) {
            throw new DomainException("Cannot confirm an appointment in status: " + this.status);
        }
        this.status = AppointmentStatus.CONFIRMED;
        this.updatedAt = Instant.now();
    }

    public void start() {
        if (this.status != AppointmentStatus.CONFIRMED && this.status != AppointmentStatus.SCHEDULED) {
            throw new DomainException("Cannot start an appointment in status: " + this.status);
        }
        this.status = AppointmentStatus.IN_PROGRESS;
        this.updatedAt = Instant.now();
    }

    public void complete() {
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new DomainException("Cannot complete an appointment that has been cancelled");
        }
        if (this.status == AppointmentStatus.COMPLETED) {
            return; // idempotent completion
        }
        this.status = AppointmentStatus.COMPLETED;
        this.updatedAt = Instant.now();
    }

    public void cancel(String reason) {
        if (this.status == AppointmentStatus.COMPLETED) {
            throw new DomainException("Cannot cancel an appointment that has already been completed");
        }
        if (this.status == AppointmentStatus.CANCELLED) {
            throw new DomainException("Appointment is already cancelled");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason is required");
        }
        this.status = AppointmentStatus.CANCELLED;
        this.cancellationReason = reason.trim();
        this.updatedAt = Instant.now();
    }

    public boolean overlapsWith(LocalDate checkDate, LocalTime checkStart, LocalTime checkEnd) {
        if (!this.appointmentDate.equals(checkDate) || this.status == AppointmentStatus.CANCELLED) {
            return false;
        }
        return this.startTime.isBefore(checkEnd) && checkStart.isBefore(this.endTime);
    }

    public Instant getStartInstant() {
        return appointmentDate.atTime(startTime).toInstant(ZoneOffset.UTC);
    }

    public Instant getEndInstant() {
        return appointmentDate.atTime(endTime).toInstant(ZoneOffset.UTC);
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public LocalDate getAppointmentDate() {
        return appointmentDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}

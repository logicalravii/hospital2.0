package com.management.hospital.consultation.domain;

import com.management.hospital.common.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "consultation")
public class Consultation {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "appointment_id", nullable = false, unique = true, updatable = false)
    private UUID appointmentId;

    @Column(name = "patient_id", nullable = false, updatable = false)
    private UUID patientId;

    @Column(name = "doctor_id", nullable = false, updatable = false)
    private UUID doctorId;

    @Column(name = "chief_complaint", nullable = false, columnDefinition = "text")
    private String chiefComplaint;

    @Column(name = "diagnosis", nullable = false, columnDefinition = "text")
    private String diagnosis;

    @Column(name = "clinical_notes", columnDefinition = "text")
    private String clinicalNotes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "vitals", columnDefinition = "jsonb")
    private Vitals vitals;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Consultation() {
        // JPA requirement
    }

    public Consultation(
            UUID appointmentId,
            UUID patientId,
            UUID doctorId,
            String chiefComplaint,
            String diagnosis,
            String clinicalNotes,
            Vitals vitals) {

        if (appointmentId == null) {
            throw new DomainException("Appointment ID is required for consultation");
        }
        if (patientId == null) {
            throw new DomainException("Patient ID is required for consultation");
        }
        if (doctorId == null) {
            throw new DomainException("Doctor ID is required for consultation");
        }
        if (chiefComplaint == null || chiefComplaint.isBlank()) {
            throw new DomainException("Chief complaint cannot be empty");
        }
        if (diagnosis == null || diagnosis.isBlank()) {
            throw new DomainException("Diagnosis cannot be empty");
        }

        this.id = UUID.randomUUID();
        this.appointmentId = appointmentId;
        this.patientId = patientId;
        this.doctorId = doctorId;
        this.chiefComplaint = chiefComplaint.trim();
        this.diagnosis = diagnosis.trim();
        this.clinicalNotes = clinicalNotes != null ? clinicalNotes.trim() : null;
        this.vitals = vitals;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAppointmentId() {
        return appointmentId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public UUID getDoctorId() {
        return doctorId;
    }

    public String getChiefComplaint() {
        return chiefComplaint;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public String getClinicalNotes() {
        return clinicalNotes;
    }

    public Vitals getVitals() {
        return vitals;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Consultation that = (Consultation) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

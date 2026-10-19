package com.management.hospital.consultation.domain;

import com.management.hospital.common.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConsultationTest {

    @Test
    @DisplayName("Should create valid Consultation aggregate")
    void shouldCreateValidConsultation() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        Vitals vitals = new Vitals(37.2, 120, 80, 75, 18, 99.0);

        Consultation consultation = new Consultation(
                appointmentId,
                patientId,
                doctorId,
                "Chest tightness and cough",
                "Acute bronchitis",
                "Prescribed salbutamol inhaler and rest",
                vitals
        );

        assertThat(consultation.getId()).isNotNull();
        assertThat(consultation.getAppointmentId()).isEqualTo(appointmentId);
        assertThat(consultation.getPatientId()).isEqualTo(patientId);
        assertThat(consultation.getDoctorId()).isEqualTo(doctorId);
        assertThat(consultation.getChiefComplaint()).isEqualTo("Chest tightness and cough");
        assertThat(consultation.getDiagnosis()).isEqualTo("Acute bronchitis");
        assertThat(consultation.getClinicalNotes()).isEqualTo("Prescribed salbutamol inhaler and rest");
        assertThat(consultation.getVitals()).isEqualTo(vitals);
        assertThat(consultation.getCreatedAt()).isNotNull();
        assertThat(consultation.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should reject null appointment ID")
    void shouldRejectNullAppointmentId() {
        assertThatThrownBy(() -> new Consultation(
                null,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Fever",
                "Viral infection",
                null,
                null
        )).isInstanceOf(DomainException.class)
                .hasMessageContaining("Appointment ID is required");
    }

    @Test
    @DisplayName("Should reject null patient ID")
    void shouldRejectNullPatientId() {
        assertThatThrownBy(() -> new Consultation(
                UUID.randomUUID(),
                null,
                UUID.randomUUID(),
                "Fever",
                "Viral infection",
                null,
                null
        )).isInstanceOf(DomainException.class)
                .hasMessageContaining("Patient ID is required");
    }

    @Test
    @DisplayName("Should reject null doctor ID")
    void shouldRejectNullDoctorId() {
        assertThatThrownBy(() -> new Consultation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                "Fever",
                "Viral infection",
                null,
                null
        )).isInstanceOf(DomainException.class)
                .hasMessageContaining("Doctor ID is required");
    }

    @Test
    @DisplayName("Should reject empty or blank chief complaint")
    void shouldRejectBlankChiefComplaint() {
        assertThatThrownBy(() -> new Consultation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "   ",
                "Viral infection",
                null,
                null
        )).isInstanceOf(DomainException.class)
                .hasMessageContaining("Chief complaint cannot be empty");
    }

    @Test
    @DisplayName("Should reject empty or blank diagnosis")
    void shouldRejectBlankDiagnosis() {
        assertThatThrownBy(() -> new Consultation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Fever",
                "",
                null,
                null
        )).isInstanceOf(DomainException.class)
                .hasMessageContaining("Diagnosis cannot be empty");
    }
}

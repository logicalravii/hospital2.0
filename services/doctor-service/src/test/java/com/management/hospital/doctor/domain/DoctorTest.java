package com.management.hospital.doctor.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DoctorTest {

    @Test
    @DisplayName("Should instantiate a valid Doctor with active status")
    void shouldCreateValidDoctor() {
        Doctor doctor = new Doctor(
            UUID.randomUUID(),
            "MED-12345",
            "Gregory",
            "House",
            Specialty.GENERAL_MEDICINE,
            "house@princeton.edu",
            "+16095551234",
            new BigDecimal("250.00"),
            "Room 101"
        );

        assertThat(doctor.getId()).isNotNull();
        assertThat(doctor.getLicenseNumber()).isEqualTo("MED-12345");
        assertThat(doctor.getFullName()).isEqualTo("Gregory House");
        assertThat(doctor.getSpecialty()).isEqualTo(Specialty.GENERAL_MEDICINE);
        assertThat(doctor.isActive()).isTrue();
        assertThat(doctor.getConsultationFee()).isEqualByComparingTo("250.00");
    }

    @Test
    @DisplayName("Should reject blank license number")
    void shouldRejectBlankLicense() {
        assertThatThrownBy(() -> new Doctor(
            null,
            "",
            "James",
            "Wilson",
            Specialty.ONCOLOGY,
            "wilson@princeton.edu",
            "+16095555678",
            BigDecimal.TEN,
            null
        )).isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("License number cannot be blank");
    }

    @Test
    @DisplayName("Should update profile and record new timestamp")
    void shouldUpdateProfile() {
        Doctor doctor = new Doctor(
            null,
            "MED-99999",
            "Lisa",
            "Cuddy",
            Specialty.GENERAL_MEDICINE,
            "cuddy@princeton.edu",
            "+16095559000",
            new BigDecimal("300.00"),
            "Dean Office"
        );

        doctor.updateProfile("Lisa", "Cuddy", Specialty.GENERAL_MEDICINE, "+16095559999", new BigDecimal("350.00"), "Suite 1A");

        assertThat(doctor.getPhone()).isEqualTo("+16095559999");
        assertThat(doctor.getConsultationFee()).isEqualByComparingTo("350.00");
        assertThat(doctor.getRoomNumber()).isEqualTo("Suite 1A");
    }

    @Test
    @DisplayName("Should deactivate and reactivate doctor")
    void shouldDeactivateAndReactivate() {
        Doctor doctor = new Doctor(
            null,
            "MED-77777",
            "Eric",
            "Foreman",
            Specialty.NEUROLOGY,
            "foreman@princeton.edu",
            "+16095557777",
            new BigDecimal("200.00"),
            null
        );

        doctor.deactivate();
        assertThat(doctor.isActive()).isFalse();

        doctor.activate();
        assertThat(doctor.isActive()).isTrue();
    }
}

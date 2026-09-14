package com.management.hospital.patient.domain;

import com.management.hospital.common.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PatientTest {

    @Test
    @DisplayName("Should successfully instantiate Patient with valid invariants")
    void shouldCreatePatient() {
        Address address = new Address("123 Main St", "Springfield", "12345");
        EmergencyContact contact = new EmergencyContact("Jane Doe", "+1987654321", "Spouse");

        Patient patient = new Patient(
            "MRN-20260909-123456",
            "John",
            "Doe",
            LocalDate.of(1990, 5, 15),
            Gender.MALE,
            "+1234567890",
            "john.doe@example.com",
            address,
            contact
        );

        assertThat(patient.getMrn()).isEqualTo("MRN-20260909-123456");
        assertThat(patient.getFirstName()).isEqualTo("John");
        assertThat(patient.getFullName()).isEqualTo("John Doe");
        assertThat(patient.getDateOfBirth()).isEqualTo(LocalDate.of(1990, 5, 15));
        assertThat(patient.getGender()).isEqualTo(Gender.MALE);
        assertThat(patient.getAddress().getCity()).isEqualTo("Springfield");
        assertThat(patient.getEmergencyContact().getName()).isEqualTo("Jane Doe");
        assertThat(patient.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should fail when date of birth is in the future")
    void shouldFailWhenDOBInFuture() {
        Address address = new Address("123 Main St", "Springfield", "12345");
        EmergencyContact contact = new EmergencyContact("Jane Doe", "+1987654321", "Spouse");

        assertThatThrownBy(() -> new Patient(
            "MRN-123",
            "John",
            "Doe",
            LocalDate.now().plusDays(1),
            Gender.MALE,
            "+1234567890",
            "john@example.com",
            address,
            contact
        ))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Date of birth cannot be in the future");
    }

    @Test
    @DisplayName("Should fail when first or last name is blank")
    void shouldFailWhenNamesBlank() {
        Address address = new Address("123 Main St", "Springfield", "12345");
        EmergencyContact contact = new EmergencyContact("Jane Doe", "+1987654321", "Spouse");

        assertThatThrownBy(() -> new Patient(
            "MRN-123",
            "   ",
            "Doe",
            LocalDate.of(1990, 1, 1),
            Gender.MALE,
            "+1234567890",
            "john@example.com",
            address,
            contact
        )).isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> new Patient(
            "MRN-123",
            "John",
            "",
            LocalDate.of(1990, 1, 1),
            Gender.MALE,
            "+1234567890",
            "john@example.com",
            address,
            contact
        )).isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Should update demographics correctly")
    void shouldUpdateDemographics() {
        Patient patient = new Patient(
            "MRN-123",
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1),
            Gender.MALE,
            "+1234567890",
            "john@example.com",
            null,
            null
        );

        patient.updateDemographics("Johnny", "Doe", "+1999999999", "johnny@example.com", new Address("456 Elm St", "Gotham", "99999"));

        assertThat(patient.getFirstName()).isEqualTo("Johnny");
        assertThat(patient.getPhone()).isEqualTo("+1999999999");
        assertThat(patient.getEmail()).isEqualTo("johnny@example.com");
        assertThat(patient.getAddress().getCity()).isEqualTo("Gotham");
    }
}

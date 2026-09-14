package com.management.hospital.patient.api.dto;

import com.management.hospital.patient.domain.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record CreatePatientRequest(
    @NotBlank(message = "First name is required")
    String firstName,

    @NotBlank(message = "Last name is required")
    String lastName,

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    LocalDate dateOfBirth,

    @NotNull(message = "Gender is required")
    Gender gender,

    @NotBlank(message = "Phone number is required")
    String phone,

    @Email(message = "Email must be valid")
    String email,

    String addressLine1,
    String city,
    String postalCode,

    String emergencyContactName,
    String emergencyContactPhone,
    String emergencyContactRelation
) {}

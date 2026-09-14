package com.management.hospital.patient.api.dto;

import com.management.hospital.patient.domain.Gender;
import com.management.hospital.patient.domain.Patient;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PatientResponse(
    UUID id,
    String mrn,
    String firstName,
    String lastName,
    String fullName,
    LocalDate dateOfBirth,
    Gender gender,
    String phone,
    String email,
    String addressLine1,
    String city,
    String postalCode,
    String emergencyContactName,
    String emergencyContactPhone,
    String emergencyContactRelation,
    Instant createdAt
) {
    public static PatientResponse from(Patient patient) {
        String line1 = patient.getAddress() != null ? patient.getAddress().getLine1() : null;
        String city = patient.getAddress() != null ? patient.getAddress().getCity() : null;
        String postalCode = patient.getAddress() != null ? patient.getAddress().getPostalCode() : null;

        String contactName = patient.getEmergencyContact() != null ? patient.getEmergencyContact().getName() : null;
        String contactPhone = patient.getEmergencyContact() != null ? patient.getEmergencyContact().getPhone() : null;
        String contactRelation = patient.getEmergencyContact() != null ? patient.getEmergencyContact().getRelationship() : null;

        return new PatientResponse(
            patient.getId(),
            patient.getMrn(),
            patient.getFirstName(),
            patient.getLastName(),
            patient.getFullName(),
            patient.getDateOfBirth(),
            patient.getGender(),
            patient.getPhone(),
            patient.getEmail(),
            line1,
            city,
            postalCode,
            contactName,
            contactPhone,
            contactRelation,
            patient.getCreatedAt()
        );
    }
}

package com.management.hospital.doctor.api.dto;

import com.management.hospital.doctor.domain.Doctor;
import com.management.hospital.doctor.domain.Specialty;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DoctorResponse(
    UUID id,
    UUID userId,
    String licenseNumber,
    String firstName,
    String lastName,
    String fullName,
    Specialty specialty,
    String email,
    String phone,
    BigDecimal consultationFee,
    String roomNumber,
    boolean active,
    Instant createdAt
) {
    public static DoctorResponse from(Doctor doctor) {
        return new DoctorResponse(
            doctor.getId(),
            doctor.getUserId(),
            doctor.getLicenseNumber(),
            doctor.getFirstName(),
            doctor.getLastName(),
            doctor.getFullName(),
            doctor.getSpecialty(),
            doctor.getEmail(),
            doctor.getPhone(),
            doctor.getConsultationFee(),
            doctor.getRoomNumber(),
            doctor.isActive(),
            doctor.getCreatedAt()
        );
    }
}

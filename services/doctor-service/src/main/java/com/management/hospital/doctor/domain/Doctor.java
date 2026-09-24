package com.management.hospital.doctor.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "doctor")
public class Doctor {

    @Id
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "license_number", nullable = false, unique = true, length = 50)
    private String licenseNumber;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "specialty", nullable = false, length = 50)
    private Specialty specialty;

    @Column(name = "email", nullable = false, unique = true, length = 150)
    private String email;

    @Column(name = "phone", nullable = false, unique = true, length = 30)
    private String phone;

    @Column(name = "consultation_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal consultationFee;

    @Column(name = "room_number", length = 20)
    private String roomNumber;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Doctor() {
        // Required by JPA
    }

    public Doctor(
        UUID userId,
        String licenseNumber,
        String firstName,
        String lastName,
        Specialty specialty,
        String email,
        String phone,
        BigDecimal consultationFee,
        String roomNumber
    ) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.licenseNumber = validateNonBlank(licenseNumber, "License number cannot be blank");
        this.firstName = validateNonBlank(firstName, "First name cannot be blank");
        this.lastName = validateNonBlank(lastName, "Last name cannot be blank");
        this.specialty = Objects.requireNonNull(specialty, "Specialty is required");
        this.email = validateNonBlank(email, "Email cannot be blank");
        this.phone = validateNonBlank(phone, "Phone number cannot be blank");
        this.consultationFee = (consultationFee != null && consultationFee.compareTo(BigDecimal.ZERO) >= 0)
            ? consultationFee
            : BigDecimal.ZERO;
        this.roomNumber = roomNumber != null ? roomNumber.trim() : null;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    private String validateNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    public void updateProfile(
        String firstName,
        String lastName,
        Specialty specialty,
        String phone,
        BigDecimal consultationFee,
        String roomNumber
    ) {
        this.firstName = validateNonBlank(firstName, "First name cannot be blank");
        this.lastName = validateNonBlank(lastName, "Last name cannot be blank");
        this.specialty = Objects.requireNonNull(specialty, "Specialty is required");
        this.phone = validateNonBlank(phone, "Phone number cannot be blank");
        if (consultationFee != null && consultationFee.compareTo(BigDecimal.ZERO) >= 0) {
            this.consultationFee = consultationFee;
        }
        this.roomNumber = roomNumber != null ? roomNumber.trim() : null;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public Specialty getSpecialty() {
        return specialty;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public BigDecimal getConsultationFee() {
        return consultationFee;
    }

    public String getRoomNumber() {
        return roomNumber;
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

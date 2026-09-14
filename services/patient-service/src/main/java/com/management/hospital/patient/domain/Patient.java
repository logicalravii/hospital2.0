package com.management.hospital.patient.domain;

import com.management.hospital.common.error.DomainException;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "patient")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 50, updatable = false)
    private String mrn;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Embedded
    private Address address;

    @Embedded
    private EmergencyContact emergencyContact;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Patient() {
        // JPA required
    }

    public Patient(
        String mrn,
        String firstName,
        String lastName,
        LocalDate dateOfBirth,
        Gender gender,
        String phone,
        String email,
        Address address,
        EmergencyContact emergencyContact
    ) {
        if (mrn == null || mrn.isBlank()) {
            throw new DomainException("Medical Record Number (MRN) cannot be blank");
        }
        if (firstName == null || firstName.isBlank()) {
            throw new DomainException("Patient first name cannot be blank");
        }
        if (lastName == null || lastName.isBlank()) {
            throw new DomainException("Patient last name cannot be blank");
        }
        if (dateOfBirth == null) {
            throw new DomainException("Date of birth is required");
        }
        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw new DomainException("Date of birth cannot be in the future");
        }
        if (gender == null) {
            throw new DomainException("Gender is required");
        }
        if (phone == null || phone.isBlank()) {
            throw new DomainException("Contact phone cannot be blank");
        }

        this.mrn = mrn.trim().toUpperCase();
        this.firstName = firstName.trim();
        this.lastName = lastName.trim();
        this.dateOfBirth = dateOfBirth;
        this.gender = gender;
        this.phone = phone.trim();
        this.email = email != null && !email.isBlank() ? email.trim().toLowerCase() : null;
        this.address = address;
        this.emergencyContact = emergencyContact;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getMrn() {
        return mrn;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public Gender getGender() {
        return gender;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public Address getAddress() {
        return address;
    }

    public EmergencyContact getEmergencyContact() {
        return emergencyContact;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void updateDemographics(String firstName, String lastName, String phone, String email, Address address) {
        if (firstName != null && !firstName.isBlank()) {
            this.firstName = firstName.trim();
        }
        if (lastName != null && !lastName.isBlank()) {
            this.lastName = lastName.trim();
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone.trim();
        }
        if (email != null && !email.isBlank()) {
            this.email = email.trim().toLowerCase();
        }
        if (address != null) {
            this.address = address;
        }
        this.updatedAt = Instant.now();
    }

    public void updateEmergencyContact(EmergencyContact emergencyContact) {
        this.emergencyContact = emergencyContact;
        this.updatedAt = Instant.now();
    }
}

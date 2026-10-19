package com.management.hospital.consultation.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RecordConsultationRequest(
    @NotNull(message = "Appointment ID is required")
    UUID appointmentId,

    @NotNull(message = "Patient ID is required")
    UUID patientId,

    @NotNull(message = "Doctor ID is required")
    UUID doctorId,

    @NotBlank(message = "Chief complaint is required")
    String chiefComplaint,

    @NotBlank(message = "Diagnosis is required")
    String diagnosis,

    String clinicalNotes,

    VitalsDto vitals
) {}

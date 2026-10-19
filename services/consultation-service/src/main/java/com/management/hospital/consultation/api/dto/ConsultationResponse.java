package com.management.hospital.consultation.api.dto;

import com.management.hospital.consultation.domain.Consultation;

import java.time.Instant;
import java.util.UUID;

public record ConsultationResponse(
    UUID id,
    UUID appointmentId,
    UUID patientId,
    UUID doctorId,
    String chiefComplaint,
    String diagnosis,
    String clinicalNotes,
    VitalsDto vitals,
    Instant createdAt,
    Instant updatedAt
) {
    public static ConsultationResponse from(Consultation consultation) {
        return new ConsultationResponse(
            consultation.getId(),
            consultation.getAppointmentId(),
            consultation.getPatientId(),
            consultation.getDoctorId(),
            consultation.getChiefComplaint(),
            consultation.getDiagnosis(),
            consultation.getClinicalNotes(),
            VitalsDto.fromDomain(consultation.getVitals()),
            consultation.getCreatedAt(),
            consultation.getUpdatedAt()
        );
    }
}

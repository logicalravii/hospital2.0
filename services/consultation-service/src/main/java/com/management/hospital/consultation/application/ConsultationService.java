package com.management.hospital.consultation.application;

import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.common.event.ConsultationCompletedEvent;
import com.management.hospital.consultation.api.dto.ConsultationResponse;
import com.management.hospital.consultation.api.dto.RecordConsultationRequest;
import com.management.hospital.consultation.domain.Consultation;
import com.management.hospital.consultation.domain.AppointmentProjection;
import com.management.hospital.consultation.domain.AppointmentProjectionRepository;
import com.management.hospital.consultation.domain.ConsultationRepository;
import com.management.hospital.consultation.domain.Vitals;
import com.management.hospital.consultation.infrastructure.ConsultationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ConsultationService {

    private static final Logger log = LoggerFactory.getLogger(ConsultationService.class);

    private final ConsultationRepository consultationRepository;
    private final ConsultationEventPublisher eventPublisher;
    private final AppointmentProjectionRepository appointmentProjectionRepository;

    public ConsultationService(
            ConsultationRepository consultationRepository,
            ConsultationEventPublisher eventPublisher,
            AppointmentProjectionRepository appointmentProjectionRepository) {
        this.consultationRepository = consultationRepository;
        this.eventPublisher = eventPublisher;
        this.appointmentProjectionRepository = appointmentProjectionRepository;
    }

    @Transactional
    public ConsultationResponse recordConsultation(RecordConsultationRequest request) {
        log.info("Recording consultation for appointmentId: {}, patientId: {}, doctorId: {}",
                request.appointmentId(), request.patientId(), request.doctorId());

        if (consultationRepository.existsByAppointmentId(request.appointmentId())) {
            throw new ConflictException(
                    "Consultation has already been recorded for appointment: " + request.appointmentId());
        }

        AppointmentProjection appointment = appointmentProjectionRepository.findById(request.appointmentId())
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", request.appointmentId()));

        if ("CANCELLED".equalsIgnoreCase(appointment.getStatus())) {
            throw new ConflictException(
                    "Cannot record consultation for a CANCELLED appointment: " + request.appointmentId());
        }

        if ("COMPLETED".equalsIgnoreCase(appointment.getStatus())) {
            throw new ConflictException(
                    "Cannot record consultation for an appointment that is already COMPLETED: " + request.appointmentId());
        }

        if (!appointment.getPatientId().equals(request.patientId())) {
            throw new ConflictException(
                    "Patient ID %s does not match the patient associated with appointment %s (%s)"
                            .formatted(request.patientId(), request.appointmentId(), appointment.getPatientId()));
        }

        if (!appointment.getDoctorId().equals(request.doctorId())) {
            throw new ConflictException(
                    "Doctor ID %s does not match the doctor associated with appointment %s (%s)"
                            .formatted(request.doctorId(), request.appointmentId(), appointment.getDoctorId()));
        }

        Vitals vitals = request.vitals() != null ? request.vitals().toDomain() : null;

        Consultation consultation = new Consultation(
                request.appointmentId(),
                request.patientId(),
                request.doctorId(),
                request.chiefComplaint(),
                request.diagnosis(),
                request.clinicalNotes(),
                vitals
        );

        Consultation saved = consultationRepository.save(consultation);
        log.info("Successfully saved consultation with ID: {}", saved.getId());

        appointment.updateStatus("COMPLETED");
        appointmentProjectionRepository.save(appointment);

        ConsultationCompletedEvent completedEvent = new ConsultationCompletedEvent(
                saved.getId(),
                saved.getAppointmentId(),
                saved.getPatientId(),
                saved.getDoctorId(),
                saved.getDiagnosis()
        );
        eventPublisher.publishConsultationCompleted(completedEvent);

        return ConsultationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public ConsultationResponse getConsultationById(UUID id) {
        return consultationRepository.findById(id)
                .map(ConsultationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation", id));
    }

    @Transactional(readOnly = true)
    public ConsultationResponse getConsultationByAppointmentId(UUID appointmentId) {
        return consultationRepository.findByAppointmentId(appointmentId)
                .map(ConsultationResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Consultation for appointment", appointmentId));
    }

    @Transactional(readOnly = true)
    public Page<ConsultationResponse> getConsultationsByPatientId(UUID patientId, Pageable pageable) {
        return consultationRepository.findByPatientId(patientId, pageable)
                .map(ConsultationResponse::from);
    }
}

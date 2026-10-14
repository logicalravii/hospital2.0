package com.management.hospital.appointment.application;

import com.management.hospital.appointment.api.dto.AppointmentResponse;
import com.management.hospital.appointment.api.dto.BookAppointmentRequest;
import com.management.hospital.appointment.api.dto.CancelAppointmentRequest;
import com.management.hospital.appointment.domain.Appointment;
import com.management.hospital.appointment.domain.AppointmentStatus;
import com.management.hospital.appointment.infrastructure.AppointmentEventPublisher;
import com.management.hospital.appointment.infrastructure.AppointmentRepository;
import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.common.event.AppointmentCancelledEvent;
import com.management.hospital.common.event.AppointmentInitiatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentEventPublisher eventPublisher;
    private final com.management.hospital.appointment.infrastructure.outbox.OutboxService outboxService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentEventPublisher eventPublisher,
            com.management.hospital.appointment.infrastructure.outbox.OutboxService outboxService) {
        this.appointmentRepository = appointmentRepository;
        this.eventPublisher = eventPublisher;
        this.outboxService = outboxService;
    }

    @Transactional
    public AppointmentResponse bookAppointment(BookAppointmentRequest request) {

        List<Appointment> existingAppointments = appointmentRepository.findByDoctorIdAndAppointmentDateAndStatusNot(
                request.doctorId(),
                request.appointmentDate(),
                AppointmentStatus.CANCELLED);

        for (Appointment existing : existingAppointments) {
            if (existing.overlapsWith(request.appointmentDate(), request.startTime(), request.endTime())) {
                throw new ConflictException("Doctor already has an active appointment during %s %s-%s".formatted(
                        request.appointmentDate(), existing.getStartTime(), existing.getEndTime()));
            }
        }

        // 3. Create appointment
        Appointment appointment = new Appointment(
                request.patientId(),
                request.doctorId(),
                request.appointmentDate(),
                request.startTime(),
                request.endTime(),
                request.reason(),
                AppointmentStatus.PENDING_VALIDATION);

        Appointment saved = appointmentRepository.save(appointment);
        log.info("Started Booking appointment {} for patient {} and doctor {} on {}",
                saved.getId(), saved.getPatientId(), saved.getDoctorId(), saved.getAppointmentDate());

        // 4. Save to Transactional Outbox (atomically committed with appointment)
        String apptType = (request.appointmentType() != null && !request.appointmentType().isBlank())
                ? request.appointmentType().trim()
                : "GENERAL_CONSULTATION";

        AppointmentInitiatedEvent event = new AppointmentInitiatedEvent(saved.getId(),
                saved.getPatientId(),
                saved.getDoctorId(),
                request.startTime(),
                request.endTime(),
                request.appointmentDate(),
                apptType);

        outboxService.saveEvent(
                "Appointment",
                saved.getId(),
                event.eventType(),
                AppointmentInitiatedEvent.TOPIC,
                event);

        return AppointmentResponse.from(saved);
    }

    @Transactional
    public AppointmentResponse confirmAppointment(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));

        appointment.confirm();
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Confirmed appointment {}", saved.getId());
        return AppointmentResponse.from(saved);
    }

    @Transactional
    public AppointmentResponse startAppointment(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));

        appointment.start();
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Started appointment {}", saved.getId());
        return AppointmentResponse.from(saved);
    }

    @Transactional
    public AppointmentResponse completeAppointment(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));

        appointment.complete();
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Completed appointment {}", saved.getId());
        return AppointmentResponse.from(saved);
    }

    @Transactional
    public AppointmentResponse cancelAppointment(UUID id, CancelAppointmentRequest request) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));

        appointment.cancel(request.reason());
        Appointment saved = appointmentRepository.save(appointment);
        log.info("Cancelled appointment {} with reason: {}", saved.getId(), request.reason());

        // Publish Kafka Cancellation Event
        AppointmentCancelledEvent event = new AppointmentCancelledEvent(
                saved.getId(),
                saved.getPatientId(),
                saved.getDoctorId(),
                request.reason());
        eventPublisher.publishAppointmentCancelled(event);

        return AppointmentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getAppointmentById(UUID id) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Appointment", id));
        return AppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public Page<AppointmentResponse> getAppointments(
            UUID doctorId,
            UUID patientId,
            LocalDate date,
            AppointmentStatus status,
            Pageable pageable) {
        Page<Appointment> page;
        if (doctorId != null) {
            page = appointmentRepository.findByDoctorId(doctorId, pageable);
        } else if (patientId != null) {
            page = appointmentRepository.findByPatientId(patientId, pageable);
        } else if (date != null) {
            page = appointmentRepository.findByAppointmentDate(date, pageable);
        } else if (status != null) {
            page = appointmentRepository.findByStatus(status, pageable);
        } else {
            page = appointmentRepository.findAll(pageable);
        }
        return page.map(AppointmentResponse::from);
    }
}

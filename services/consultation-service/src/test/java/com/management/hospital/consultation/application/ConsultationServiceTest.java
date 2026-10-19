package com.management.hospital.consultation.application;

import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.common.event.ConsultationCompletedEvent;
import com.management.hospital.consultation.api.dto.ConsultationResponse;
import com.management.hospital.consultation.api.dto.RecordConsultationRequest;
import com.management.hospital.consultation.api.dto.VitalsDto;
import com.management.hospital.consultation.domain.Consultation;
import com.management.hospital.consultation.domain.ConsultationRepository;
import com.management.hospital.consultation.domain.Vitals;
import com.management.hospital.consultation.infrastructure.ConsultationEventPublisher;
import com.management.hospital.consultation.domain.AppointmentProjection;
import com.management.hospital.consultation.domain.AppointmentProjectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultationServiceTest {

    @Mock
    private ConsultationRepository consultationRepository;

    @Mock
    private ConsultationEventPublisher eventPublisher;

    @Mock
    private AppointmentProjectionRepository appointmentProjectionRepository;

    private ConsultationService consultationService;

    @BeforeEach
    void setUp() {
        consultationService = new ConsultationService(consultationRepository, eventPublisher, appointmentProjectionRepository);
    }

    @Test
    @DisplayName("Should successfully record consultation and publish ConsultationCompletedEvent")
    void shouldRecordConsultationSuccessfully() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        RecordConsultationRequest request = new RecordConsultationRequest(
                appointmentId,
                patientId,
                doctorId,
                "Persistent migraine and light sensitivity",
                "Migraine without aura",
                "Prescribed sumatriptan 50mg PRN",
                new VitalsDto(36.9, 118, 76, 68, 14, 99.0)
        );

        AppointmentProjection projection = new AppointmentProjection(appointmentId, patientId, doctorId, "CONFIRMED");

        when(consultationRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentProjectionRepository.findById(appointmentId)).thenReturn(Optional.of(projection));
        when(consultationRepository.save(any(Consultation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ConsultationResponse response = consultationService.recordConsultation(request);

        assertThat(response).isNotNull();
        assertThat(response.id()).isNotNull();
        assertThat(response.appointmentId()).isEqualTo(appointmentId);
        assertThat(response.patientId()).isEqualTo(patientId);
        assertThat(response.doctorId()).isEqualTo(doctorId);
        assertThat(response.diagnosis()).isEqualTo("Migraine without aura");
        assertThat(response.vitals()).isNotNull();
        assertThat(response.vitals().temperatureCelsius()).isEqualTo(36.9);
        assertThat(projection.getStatus()).isEqualTo("COMPLETED");

        verify(appointmentProjectionRepository).save(projection);

        ArgumentCaptor<ConsultationCompletedEvent> eventCaptor = ArgumentCaptor.forClass(ConsultationCompletedEvent.class);
        verify(eventPublisher).publishConsultationCompleted(eventCaptor.capture());

        ConsultationCompletedEvent captured = eventCaptor.getValue();
        assertThat(captured.consultationId()).isEqualTo(response.id());
        assertThat(captured.appointmentId()).isEqualTo(appointmentId);
        assertThat(captured.patientId()).isEqualTo(patientId);
        assertThat(captured.doctorId()).isEqualTo(doctorId);
        assertThat(captured.diagnosis()).isEqualTo("Migraine without aura");
    }

    @Test
    @DisplayName("Should throw ConflictException when consultation already exists for the appointment")
    void shouldRejectDuplicateConsultationForAppointment() {
        UUID appointmentId = UUID.randomUUID();
        RecordConsultationRequest request = new RecordConsultationRequest(
                appointmentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Fever",
                "Flu",
                null,
                null
        );

        when(consultationRepository.existsByAppointmentId(appointmentId)).thenReturn(true);

        assertThatThrownBy(() -> consultationService.recordConsultation(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Consultation has already been recorded for appointment");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when appointment projection does not exist")
    void shouldRejectWhenAppointmentDoesNotExist() {
        UUID appointmentId = UUID.randomUUID();
        RecordConsultationRequest request = new RecordConsultationRequest(
                appointmentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Fever",
                "Flu",
                null,
                null
        );

        when(consultationRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentProjectionRepository.findById(appointmentId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultationService.recordConsultation(request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Appointment");
    }

    @Test
    @DisplayName("Should throw ConflictException when appointment is CANCELLED")
    void shouldRejectWhenAppointmentIsCancelled() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        RecordConsultationRequest request = new RecordConsultationRequest(
                appointmentId,
                patientId,
                doctorId,
                "Fever",
                "Flu",
                null,
                null
        );

        AppointmentProjection projection = new AppointmentProjection(appointmentId, patientId, doctorId, "CANCELLED");

        when(consultationRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentProjectionRepository.findById(appointmentId)).thenReturn(Optional.of(projection));

        assertThatThrownBy(() -> consultationService.recordConsultation(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot record consultation for a CANCELLED appointment");
    }

    @Test
    @DisplayName("Should throw ConflictException when appointment is already COMPLETED")
    void shouldRejectWhenAppointmentIsCompleted() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        RecordConsultationRequest request = new RecordConsultationRequest(
                appointmentId,
                patientId,
                doctorId,
                "Fever",
                "Flu",
                null,
                null
        );

        AppointmentProjection projection = new AppointmentProjection(appointmentId, patientId, doctorId, "COMPLETED");

        when(consultationRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentProjectionRepository.findById(appointmentId)).thenReturn(Optional.of(projection));

        assertThatThrownBy(() -> consultationService.recordConsultation(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cannot record consultation for an appointment that is already COMPLETED");
    }

    @Test
    @DisplayName("Should throw ConflictException when patientId does not match appointment")
    void shouldRejectWhenPatientIdDoesNotMatchAppointment() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID differentPatientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        RecordConsultationRequest request = new RecordConsultationRequest(
                appointmentId,
                patientId,
                doctorId,
                "Fever",
                "Flu",
                null,
                null
        );

        AppointmentProjection projection = new AppointmentProjection(appointmentId, differentPatientId, doctorId, "CONFIRMED");

        when(consultationRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentProjectionRepository.findById(appointmentId)).thenReturn(Optional.of(projection));

        assertThatThrownBy(() -> consultationService.recordConsultation(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("does not match the patient associated with appointment");
    }

    @Test
    @DisplayName("Should throw ConflictException when doctorId does not match appointment")
    void shouldRejectWhenDoctorIdDoesNotMatchAppointment() {
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID differentDoctorId = UUID.randomUUID();
        RecordConsultationRequest request = new RecordConsultationRequest(
                appointmentId,
                patientId,
                doctorId,
                "Fever",
                "Flu",
                null,
                null
        );

        AppointmentProjection projection = new AppointmentProjection(appointmentId, patientId, differentDoctorId, "CONFIRMED");

        when(consultationRepository.existsByAppointmentId(appointmentId)).thenReturn(false);
        when(appointmentProjectionRepository.findById(appointmentId)).thenReturn(Optional.of(projection));

        assertThatThrownBy(() -> consultationService.recordConsultation(request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("does not match the doctor associated with appointment");
    }

    @Test
    @DisplayName("Should return consultation by ID")
    void shouldGetConsultationById() {
        Consultation consultation = new Consultation(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Knee pain",
                "Patellar tendinitis",
                "RICE protocol",
                new Vitals(36.7, 122, 78, 70, 16, 98.0)
        );

        when(consultationRepository.findById(consultation.getId())).thenReturn(Optional.of(consultation));

        ConsultationResponse response = consultationService.getConsultationById(consultation.getId());

        assertThat(response.id()).isEqualTo(consultation.getId());
        assertThat(response.diagnosis()).isEqualTo("Patellar tendinitis");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when consultation ID does not exist")
    void shouldThrowWhenConsultationNotFound() {
        UUID nonExistent = UUID.randomUUID();
        when(consultationRepository.findById(nonExistent)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultationService.getConsultationById(nonExistent))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(nonExistent.toString());
    }

    @Test
    @DisplayName("Should return consultation by appointment ID")
    void shouldGetConsultationByAppointmentId() {
        UUID appointmentId = UUID.randomUUID();
        Consultation consultation = new Consultation(
                appointmentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Annual checkup",
                "Healthy adult",
                "All vitals stable",
                null
        );

        when(consultationRepository.findByAppointmentId(appointmentId)).thenReturn(Optional.of(consultation));

        ConsultationResponse response = consultationService.getConsultationByAppointmentId(appointmentId);

        assertThat(response.appointmentId()).isEqualTo(appointmentId);
        assertThat(response.diagnosis()).isEqualTo("Healthy adult");
    }

    @Test
    @DisplayName("Should return paginated consultations by patient ID")
    void shouldGetConsultationsByPatientId() {
        UUID patientId = UUID.randomUUID();
        Consultation consultation = new Consultation(
                UUID.randomUUID(),
                patientId,
                UUID.randomUUID(),
                "Asthma flare",
                "Asthma",
                null,
                null
        );

        PageRequest pageRequest = PageRequest.of(0, 10);
        when(consultationRepository.findByPatientId(patientId, pageRequest))
                .thenReturn(new PageImpl<>(List.of(consultation)));

        Page<ConsultationResponse> page = consultationService.getConsultationsByPatientId(patientId, pageRequest);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).patientId()).isEqualTo(patientId);
    }
}

package com.management.hospital.patient.application;

import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.patient.api.dto.CreatePatientRequest;
import com.management.hospital.patient.api.dto.PatientResponse;
import com.management.hospital.patient.domain.Gender;
import com.management.hospital.patient.domain.MedicalRecordNumberGenerator;
import com.management.hospital.patient.domain.Patient;
import com.management.hospital.patient.infrastructure.PatientEventPublisher;
import com.management.hospital.patient.infrastructure.PatientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientServiceTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private MedicalRecordNumberGenerator mrnGenerator;

    @Mock
    private PatientEventPublisher eventPublisher;

    private PatientService patientService;

    @BeforeEach
    void setUp() {
        patientService = new PatientService(patientRepository, mrnGenerator, eventPublisher);
    }

    @Test
    @DisplayName("Should successfully register a patient and emit Kafka event")
    void shouldRegisterPatientAndPublishEvent() {
        CreatePatientRequest request = new CreatePatientRequest(
            "Alice",
            "Wonder",
            LocalDate.of(1995, 4, 10),
            Gender.FEMALE,
            "+1234567890",
            "alice@example.com",
            "123 Street",
            "Metropolis",
            "10001",
            "Bob Wonder",
            "+1987654321",
            "Brother"
        );

        when(patientRepository.existsByPhone("+1234567890")).thenReturn(false);
        when(mrnGenerator.generate()).thenReturn("MRN-20260909-999999");
        when(patientRepository.existsByMrn("MRN-20260909-999999")).thenReturn(false);
        when(patientRepository.save(any(Patient.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PatientResponse response = patientService.registerPatient(request);

        assertThat(response.firstName()).isEqualTo("Alice");
        assertThat(response.lastName()).isEqualTo("Wonder");
        assertThat(response.mrn()).isEqualTo("MRN-20260909-999999");
        assertThat(response.gender()).isEqualTo(Gender.FEMALE);

        verify(eventPublisher).publishPatientRegistered(any());
        verify(patientRepository).save(any(Patient.class));
    }

    @Test
    @DisplayName("Should reject registration when phone number is already registered")
    void shouldRejectDuplicatePhone() {
        CreatePatientRequest request = new CreatePatientRequest(
            "Alice",
            "Wonder",
            LocalDate.of(1995, 4, 10),
            Gender.FEMALE,
            "+1234567890",
            "alice@example.com",
            null, null, null, null, null, null
        );

        when(patientRepository.existsByPhone("+1234567890")).thenReturn(true);

        assertThatThrownBy(() -> patientService.registerPatient(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already registered");
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when fetching non-existent patient")
    void shouldThrowWhenPatientNotFound() {
        UUID id = UUID.randomUUID();
        when(patientRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> patientService.getPatientById(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Patient with identifier");
    }

    @Test
    @DisplayName("Should fetch patient by MRN successfully")
    void shouldFetchPatientByMrn() {
        String mrn = "MRN-20260909-123456";
        Patient patient = new Patient(
            mrn, "Jane", "Doe", LocalDate.of(1992, 2, 2),
            Gender.FEMALE, "+1987654321", "jane@example.com", null, null
        );
        when(patientRepository.findByMrn(mrn)).thenReturn(Optional.of(patient));

        PatientResponse response = patientService.getPatientByMrn(mrn);

        assertThat(response.mrn()).isEqualTo(mrn);
        assertThat(response.firstName()).isEqualTo("Jane");
    }
}

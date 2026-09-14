package com.management.hospital.patient.application;

import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.common.event.PatientRegisteredEvent;
import com.management.hospital.patient.api.dto.CreatePatientRequest;
import com.management.hospital.patient.api.dto.PatientResponse;
import com.management.hospital.patient.domain.Address;
import com.management.hospital.patient.domain.EmergencyContact;
import com.management.hospital.patient.domain.MedicalRecordNumberGenerator;
import com.management.hospital.patient.domain.Patient;
import com.management.hospital.patient.infrastructure.PatientEventPublisher;
import com.management.hospital.patient.infrastructure.PatientRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PatientService {

    private final PatientRepository patientRepository;
    private final MedicalRecordNumberGenerator mrnGenerator;
    private final PatientEventPublisher eventPublisher;

    public PatientService(
        PatientRepository patientRepository,
        MedicalRecordNumberGenerator mrnGenerator,
        PatientEventPublisher eventPublisher
    ) {
        this.patientRepository = patientRepository;
        this.mrnGenerator = mrnGenerator;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public PatientResponse registerPatient(CreatePatientRequest request) {
        if (patientRepository.existsByPhone(request.phone().trim())) {
            throw new ConflictException("Patient with phone number '%s' is already registered".formatted(request.phone().trim()));
        }

        String mrn = mrnGenerator.generate();
        while (patientRepository.existsByMrn(mrn)) {
            mrn = mrnGenerator.generate();
        }

        Address address = new Address(request.addressLine1(), request.city(), request.postalCode());
        EmergencyContact emergencyContact = new EmergencyContact(
            request.emergencyContactName(),
            request.emergencyContactPhone(),
            request.emergencyContactRelation()
        );

        Patient patient = new Patient(
            mrn,
            request.firstName(),
            request.lastName(),
            request.dateOfBirth(),
            request.gender(),
            request.phone(),
            request.email(),
            address,
            emergencyContact
        );

        Patient saved = patientRepository.save(patient);

        // Publish Kafka Domain Event
        PatientRegisteredEvent event = new PatientRegisteredEvent(
            saved.getId(),
            saved.getMrn(),
            saved.getFirstName(),
            saved.getLastName(),
            saved.getEmail(),
            saved.getPhone()
        );
        eventPublisher.publishPatientRegistered(event);

        return PatientResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(UUID id) {
        Patient patient = patientRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Patient", id));

        return PatientResponse.from(patient);
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientByMrn(String mrn) {
        Patient patient = patientRepository.findByMrn(mrn)
            .orElseThrow(() -> new ResourceNotFoundException("Patient with MRN '" + mrn + "' was not found"));

        return PatientResponse.from(patient);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> getAllPatients(Pageable pageable) {
        return patientRepository.findAll(pageable)
            .map(PatientResponse::from);
    }
}

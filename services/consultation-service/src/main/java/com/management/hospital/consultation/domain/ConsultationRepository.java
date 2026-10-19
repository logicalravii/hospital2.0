package com.management.hospital.consultation.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ConsultationRepository extends JpaRepository<Consultation, UUID> {

    Optional<Consultation> findByAppointmentId(UUID appointmentId);

    boolean existsByAppointmentId(UUID appointmentId);

    Page<Consultation> findByPatientId(UUID patientId, Pageable pageable);

    Page<Consultation> findByDoctorId(UUID doctorId, Pageable pageable);
}

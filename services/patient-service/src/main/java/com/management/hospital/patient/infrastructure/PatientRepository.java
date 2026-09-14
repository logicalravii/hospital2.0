package com.management.hospital.patient.infrastructure;

import com.management.hospital.patient.domain.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PatientRepository extends JpaRepository<Patient, UUID> {
    Optional<Patient> findByMrn(String mrn);
    boolean existsByMrn(String mrn);
    boolean existsByPhone(String phone);
    Page<Patient> findAll(Pageable pageable);
}

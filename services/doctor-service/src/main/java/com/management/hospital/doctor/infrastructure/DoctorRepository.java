package com.management.hospital.doctor.infrastructure;

import com.management.hospital.doctor.domain.Doctor;
import com.management.hospital.doctor.domain.Specialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    Optional<Doctor> findByLicenseNumber(String licenseNumber);
    boolean existsByLicenseNumber(String licenseNumber);
    boolean existsByEmail(String email);
    Page<Doctor> findBySpecialtyAndActiveTrue(Specialty specialty, Pageable pageable);
    Page<Doctor> findByActiveTrue(Pageable pageable);
}

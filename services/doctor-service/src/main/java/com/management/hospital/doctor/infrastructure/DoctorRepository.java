package com.management.hospital.doctor.infrastructure;

import com.management.hospital.doctor.domain.Doctor;
import com.management.hospital.doctor.domain.Specialty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    Optional<Doctor> findByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    Page<Doctor> findBySpecialtyAndActiveTrue(Specialty specialty, Pageable pageable);

    Page<Doctor> findByActiveTrue(Pageable pageable);

    // @Query("""
    // SELECT d FROM Doctor d
    // WHERE (LOWER(d.firstName) = LOWER(:firstName) OR LOWER(d.lastName) =
    // LOWER(:lastName))
    // AND d.active = true
    // """)
    // List<Doctor> findByFirstNameIgnoreCaseOrLastNameIgnoreCaseAndActive(
    // @Param("firstName") String firstName,
    // @Param("lastName") String lastName
    // );
    List<Doctor> findByFirstNameIgnoreCaseOrLastNameIgnoreCaseAndActiveTrue(String firstName, String lastName);
}

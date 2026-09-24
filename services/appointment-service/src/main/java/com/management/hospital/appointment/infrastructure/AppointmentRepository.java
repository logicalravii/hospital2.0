package com.management.hospital.appointment.infrastructure;

import com.management.hospital.appointment.domain.Appointment;
import com.management.hospital.appointment.domain.AppointmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, UUID> {
    List<Appointment> findByDoctorIdAndAppointmentDateAndStatusNot(UUID doctorId, LocalDate appointmentDate, AppointmentStatus status);
    Page<Appointment> findByDoctorId(UUID doctorId, Pageable pageable);
    Page<Appointment> findByPatientId(UUID patientId, Pageable pageable);
    Page<Appointment> findByAppointmentDate(LocalDate appointmentDate, Pageable pageable);
    Page<Appointment> findByStatus(AppointmentStatus status, Pageable pageable);
}

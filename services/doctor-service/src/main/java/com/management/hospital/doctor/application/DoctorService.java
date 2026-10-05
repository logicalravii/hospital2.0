package com.management.hospital.doctor.application;

import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.doctor.api.dto.AvailableSlotsResponse;
import com.management.hospital.doctor.api.dto.CreateDoctorRequest;
import com.management.hospital.doctor.api.dto.CreateScheduleRequest;
import com.management.hospital.doctor.api.dto.DoctorResponse;
import com.management.hospital.doctor.api.dto.DoctorScheduleResponse;
import com.management.hospital.doctor.domain.Doctor;
import com.management.hospital.doctor.domain.DoctorSchedule;
import com.management.hospital.doctor.domain.Specialty;
import com.management.hospital.doctor.domain.TimeSlot;
import com.management.hospital.doctor.infrastructure.DoctorRepository;
import com.management.hospital.doctor.infrastructure.DoctorScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class DoctorService {

    private static final Logger log = LoggerFactory.getLogger(DoctorService.class);

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleRepository scheduleRepository;

    public DoctorService(DoctorRepository doctorRepository, DoctorScheduleRepository scheduleRepository) {
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional
    public DoctorResponse registerDoctor(CreateDoctorRequest request) {
        if (doctorRepository.existsByLicenseNumber(request.licenseNumber().trim())) {
            throw new ConflictException(
                    "Doctor with license number '%s' is already registered".formatted(request.licenseNumber().trim()));
        }
        if (doctorRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new ConflictException(
                    "Doctor with email '%s' is already registered".formatted(request.email().trim()));
        }
        if (doctorRepository.existsByPhone(request.phone().trim())) {
            throw new ConflictException(
                    "Doctor with phone number '%s' is already registered".formatted(request.phone().trim()));
        }

        Doctor doctor = new Doctor(
                request.userId(),
                request.licenseNumber(),
                request.firstName(),
                request.lastName(),
                request.specialty(),
                request.email().trim().toLowerCase(),
                request.phone(),
                request.consultationFee(),
                request.roomNumber());

        Doctor saved = doctorRepository.save(doctor);
        log.info("Registered doctor '{}' with ID {} and license {}", saved.getFullName(), saved.getId(),
                saved.getLicenseNumber());
        return DoctorResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(UUID id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", id));
        return DoctorResponse.from(doctor);
    }

    @Transactional(readOnly = true)
    public List<DoctorResponse> getDoctorByName(String name) {
        List<Doctor> doctors = doctorRepository.findByFirstNameIgnoreCaseOrLastNameIgnoreCaseAndActiveTrue(name, name);
        return doctors.stream().map(DoctorResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public Page<DoctorResponse> getAllDoctors(Specialty specialty, Pageable pageable) {
        Page<Doctor> page = (specialty != null)
                ? doctorRepository.findBySpecialtyAndActiveTrue(specialty, pageable)
                : doctorRepository.findByActiveTrue(pageable);
        return page.map(DoctorResponse::from);
    }

    @Transactional
    public DoctorScheduleResponse addSchedule(UUID doctorId, CreateScheduleRequest request) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor", doctorId));

        DoctorSchedule schedule = new DoctorSchedule(
                doctor,
                request.dayOfWeek(),
                request.startTime(),
                request.endTime(),
                request.slotDurationMinutes(),
                request.maxPatients());

        DoctorSchedule saved = scheduleRepository.save(schedule);
        log.info("Configured schedule {} for doctor {} on {}", saved.getId(), doctorId, saved.getDayOfWeek());
        return DoctorScheduleResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<DoctorScheduleResponse> getDoctorSchedules(UUID doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor", doctorId);
        }
        return scheduleRepository.findByDoctorIdAndActiveTrue(doctorId)
                .stream()
                .map(DoctorScheduleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AvailableSlotsResponse getAvailableSlotsForDate(UUID doctorId, LocalDate date) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResourceNotFoundException("Doctor", doctorId);
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        List<DoctorSchedule> schedules = scheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId,
                dayOfWeek);

        List<TimeSlot> allSlots = new ArrayList<>();
        for (DoctorSchedule schedule : schedules) {
            allSlots.addAll(schedule.generateSlots());
        }

        return new AvailableSlotsResponse(doctorId, date, dayOfWeek, allSlots);
    }
}

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
import com.management.hospital.doctor.infrastructure.DoctorRepository;
import com.management.hospital.doctor.infrastructure.DoctorScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorScheduleRepository scheduleRepository;

    private DoctorService doctorService;

    @BeforeEach
    void setUp() {
        doctorService = new DoctorService(doctorRepository, scheduleRepository);
    }

    @Test
    @DisplayName("Should successfully register a doctor")
    void shouldRegisterDoctor() {
        CreateDoctorRequest request = new CreateDoctorRequest(
            null,
            "DOC-1234",
            "Robert",
            "Chase",
            Specialty.SURGERY,
            "chase@princeton.edu",
            "+16095553333",
            new BigDecimal("220.00"),
            "OR-3"
        );

        when(doctorRepository.existsByLicenseNumber("DOC-1234")).thenReturn(false);
        when(doctorRepository.existsByEmail("chase@princeton.edu")).thenReturn(false);
        when(doctorRepository.save(any(Doctor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DoctorResponse response = doctorService.registerDoctor(request);

        assertThat(response.licenseNumber()).isEqualTo("DOC-1234");
        assertThat(response.fullName()).isEqualTo("Robert Chase");
        assertThat(response.specialty()).isEqualTo(Specialty.SURGERY);
        assertThat(response.active()).isTrue();

        verify(doctorRepository).save(any(Doctor.class));
    }

    @Test
    @DisplayName("Should reject doctor registration when license already exists")
    void shouldRejectDuplicateLicense() {
        CreateDoctorRequest request = new CreateDoctorRequest(
            null,
            "DOC-1234",
            "Robert",
            "Chase",
            Specialty.SURGERY,
            "chase@princeton.edu",
            "+16095553333",
            new BigDecimal("220.00"),
            null
        );

        when(doctorRepository.existsByLicenseNumber("DOC-1234")).thenReturn(true);

        assertThatThrownBy(() -> doctorService.registerDoctor(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("license number 'DOC-1234' is already registered");
    }

    @Test
    @DisplayName("Should reject doctor registration when email already exists")
    void shouldRejectDuplicateEmail() {
        CreateDoctorRequest request = new CreateDoctorRequest(
            null,
            "DOC-5678",
            "Robert",
            "Chase",
            Specialty.SURGERY,
            "chase@princeton.edu",
            "+16095553333",
            new BigDecimal("220.00"),
            null
        );

        when(doctorRepository.existsByLicenseNumber("DOC-5678")).thenReturn(false);
        when(doctorRepository.existsByEmail("chase@princeton.edu")).thenReturn(true);

        assertThatThrownBy(() -> doctorService.registerDoctor(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("email 'chase@princeton.edu' is already registered");
    }

    @Test
    @DisplayName("Should reject doctor registration when phone already exists")
    void shouldRejectDuplicatePhone() {
        CreateDoctorRequest request = new CreateDoctorRequest(
            null,
            "DOC-5678",
            "Robert",
            "Chase",
            Specialty.SURGERY,
            "new.email@princeton.edu",
            "+16095553333",
            new BigDecimal("220.00"),
            null
        );

        when(doctorRepository.existsByLicenseNumber("DOC-5678")).thenReturn(false);
        when(doctorRepository.existsByEmail("new.email@princeton.edu")).thenReturn(false);
        when(doctorRepository.existsByPhone("+16095553333")).thenReturn(true);

        assertThatThrownBy(() -> doctorService.registerDoctor(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("phone number '+16095553333' is already registered");
    }

    @Test
    @DisplayName("Should add a valid schedule block for doctor")
    void shouldAddSchedule() {
        UUID doctorId = UUID.randomUUID();
        Doctor doctor = new Doctor(null, "DOC-1234", "Robert", "Chase", Specialty.SURGERY, "chase@princeton.edu", "+1", BigDecimal.TEN, null);

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(scheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, DayOfWeek.FRIDAY))
            .thenReturn(List.of());
        when(scheduleRepository.save(any(DoctorSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateScheduleRequest request = new CreateScheduleRequest(
            DayOfWeek.FRIDAY,
            LocalTime.of(9, 0),
            LocalTime.of(12, 0),
            30,
            6
        );

        DoctorScheduleResponse response = doctorService.addSchedule(doctorId, request);

        assertThat(response.dayOfWeek()).isEqualTo(DayOfWeek.FRIDAY);
        assertThat(response.availableSlots()).hasSize(6);
        verify(scheduleRepository).save(any(DoctorSchedule.class));
    }

    @Test
    @DisplayName("Should reject schedule that overlaps with existing active block")
    void shouldRejectOverlappingSchedule() {
        UUID doctorId = UUID.randomUUID();
        Doctor doctor = new Doctor(null, "DOC-1234", "Robert", "Chase", Specialty.SURGERY, "chase@princeton.edu", "+1", BigDecimal.TEN, null);
        DoctorSchedule existing = new DoctorSchedule(doctor, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0), 30, 6);

        when(doctorRepository.findById(doctorId)).thenReturn(Optional.of(doctor));
        when(scheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, DayOfWeek.MONDAY))
            .thenReturn(List.of(existing));

        CreateScheduleRequest request = new CreateScheduleRequest(
            DayOfWeek.MONDAY,
            LocalTime.of(10, 0),
            LocalTime.of(13, 0),
            30,
            6
        );

        assertThatThrownBy(() -> doctorService.addSchedule(doctorId, request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("Schedule overlaps with existing active block");
    }

    @Test
    @DisplayName("Should return available slots for date")
    void shouldReturnSlotsForDate() {
        UUID doctorId = UUID.randomUUID();
        Doctor doctor = new Doctor(null, "DOC-1234", "Robert", "Chase", Specialty.SURGERY, "chase@princeton.edu", "+1", BigDecimal.TEN, null);
        DoctorSchedule schedule = new DoctorSchedule(doctor, DayOfWeek.MONDAY, LocalTime.of(14, 0), LocalTime.of(16, 0), 60, 2);

        LocalDate nextMonday = LocalDate.of(2026, 9, 21); // Monday
        assertThat(nextMonday.getDayOfWeek()).isEqualTo(DayOfWeek.MONDAY);

        when(doctorRepository.existsById(doctorId)).thenReturn(true);
        when(scheduleRepository.findByDoctorIdAndDayOfWeekAndActiveTrue(doctorId, DayOfWeek.MONDAY))
            .thenReturn(List.of(schedule));

        AvailableSlotsResponse response = doctorService.getAvailableSlotsForDate(doctorId, nextMonday);

        assertThat(response.doctorId()).isEqualTo(doctorId);
        assertThat(response.date()).isEqualTo(nextMonday);
        assertThat(response.slots()).hasSize(2);
    }
}

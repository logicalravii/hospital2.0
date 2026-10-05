package com.management.hospital.doctor.api;

import com.management.hospital.doctor.api.dto.AvailableSlotsResponse;
import com.management.hospital.doctor.api.dto.CreateDoctorRequest;
import com.management.hospital.doctor.api.dto.CreateScheduleRequest;
import com.management.hospital.doctor.api.dto.DoctorResponse;
import com.management.hospital.doctor.api.dto.DoctorScheduleResponse;
import com.management.hospital.doctor.application.DoctorService;
import com.management.hospital.doctor.domain.Specialty;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    public ResponseEntity<DoctorResponse> registerDoctor(@Valid @RequestBody CreateDoctorRequest request) {
        DoctorResponse response = doctorService.registerDoctor(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> getDoctorById(@PathVariable UUID id) {
        DoctorResponse response = doctorService.getDoctorById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/name")
    public ResponseEntity<List<DoctorResponse>> getDoctorByName(@RequestParam(required = false) String name) {
        List<DoctorResponse> response = doctorService.getDoctorByName(name);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<DoctorResponse>> getAllDoctors(
            @RequestParam(required = false) Specialty specialty,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<DoctorResponse> response = doctorService.getAllDoctors(
                specialty,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "lastName")));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/schedules")
    public ResponseEntity<DoctorScheduleResponse> addSchedule(
            @PathVariable UUID id,
            @Valid @RequestBody CreateScheduleRequest request) {
        DoctorScheduleResponse response = doctorService.addSchedule(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}/schedules")
    public ResponseEntity<List<DoctorScheduleResponse>> getDoctorSchedules(@PathVariable UUID id) {
        List<DoctorScheduleResponse> response = doctorService.getDoctorSchedules(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/slots")
    public ResponseEntity<AvailableSlotsResponse> getAvailableSlots(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        AvailableSlotsResponse response = doctorService.getAvailableSlotsForDate(id, date);
        return ResponseEntity.ok(response);
    }
}

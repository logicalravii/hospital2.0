package com.management.hospital.appointment.api;

import com.management.hospital.appointment.api.dto.AppointmentResponse;
import com.management.hospital.appointment.api.dto.BookAppointmentRequest;
import com.management.hospital.appointment.api.dto.CancelAppointmentRequest;
import com.management.hospital.appointment.application.AppointmentService;
import com.management.hospital.appointment.domain.AppointmentStatus;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> bookAppointment(@Valid @RequestBody BookAppointmentRequest request) {
        AppointmentResponse response = appointmentService.bookAppointment(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<AppointmentResponse> confirmAppointment(@PathVariable UUID id) {
        AppointmentResponse response = appointmentService.confirmAppointment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<AppointmentResponse> startAppointment(@PathVariable UUID id) {
        AppointmentResponse response = appointmentService.startAppointment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(
            @PathVariable UUID id,
            @Valid @RequestBody CancelAppointmentRequest request) {
        AppointmentResponse response = appointmentService.cancelAppointment(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable UUID id) {
        AppointmentResponse response = appointmentService.getAppointmentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<AppointmentResponse>> getAppointments(
            @RequestParam(required = false) UUID doctorId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AppointmentResponse> response = appointmentService.getAppointments(
                doctorId,
                patientId,
                date,
                status,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "appointmentDate", "startTime")));
        return ResponseEntity.ok(response);
    }
}

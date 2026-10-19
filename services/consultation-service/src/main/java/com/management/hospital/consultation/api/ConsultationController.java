package com.management.hospital.consultation.api;

import com.management.hospital.consultation.api.dto.ConsultationResponse;
import com.management.hospital.consultation.api.dto.RecordConsultationRequest;
import com.management.hospital.consultation.application.ConsultationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/consultations")
public class ConsultationController {

    private final ConsultationService consultationService;

    public ConsultationController(ConsultationService consultationService) {
        this.consultationService = consultationService;
    }

    @PostMapping
    public ResponseEntity<ConsultationResponse> recordConsultation(
            @Valid @RequestBody RecordConsultationRequest request) {
        ConsultationResponse response = consultationService.recordConsultation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsultationResponse> getConsultationById(@PathVariable UUID id) {
        ConsultationResponse response = consultationService.getConsultationById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<ConsultationResponse> getConsultationByAppointmentId(
            @PathVariable UUID appointmentId) {
        ConsultationResponse response = consultationService.getConsultationByAppointmentId(appointmentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<ConsultationResponse>> getConsultationsByPatient(
            @RequestParam UUID patientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<ConsultationResponse> response = consultationService.getConsultationsByPatientId(
                patientId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return ResponseEntity.ok(response);
    }
}

package com.management.hospital.consultation.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.consultation.api.dto.ConsultationResponse;
import com.management.hospital.consultation.api.dto.RecordConsultationRequest;
import com.management.hospital.consultation.api.dto.VitalsDto;
import com.management.hospital.consultation.application.ConsultationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsultationController.class)
@Import(GlobalExceptionHandler.class)
class ConsultationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ConsultationService consultationService;

    @Test
    @DisplayName("POST /api/v1/consultations should return 201 on valid consultation recording")
    void shouldRecordConsultationSuccessfully() throws Exception {
        UUID consultationId = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        RecordConsultationRequest request = new RecordConsultationRequest(
                appointmentId,
                patientId,
                doctorId,
                "Persistent headache",
                "Tension headache",
                "Rest and analgesics",
                new VitalsDto(36.8, 120, 80, 72, 16, 98.0)
        );

        ConsultationResponse response = new ConsultationResponse(
                consultationId,
                appointmentId,
                patientId,
                doctorId,
                "Persistent headache",
                "Tension headache",
                "Rest and analgesics",
                new VitalsDto(36.8, 120, 80, 72, 16, 98.0),
                Instant.now(),
                Instant.now()
        );

        when(consultationService.recordConsultation(any(RecordConsultationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(consultationId.toString()))
                .andExpect(jsonPath("$.appointmentId").value(appointmentId.toString()))
                .andExpect(jsonPath("$.diagnosis").value("Tension headache"))
                .andExpect(jsonPath("$.vitals.systolicBp").value(120))
                .andExpect(jsonPath("$.vitals.temperatureCelsius").value(36.8));
    }

    @Test
    @DisplayName("POST /api/v1/consultations should return 400 when required fields are missing")
    void shouldReturn400WhenValidationFails() throws Exception {
        RecordConsultationRequest request = new RecordConsultationRequest(
                null,
                null,
                null,
                "",
                "",
                null,
                null
        );

        mockMvc.perform(post("/api/v1/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.appointmentId").exists())
                .andExpect(jsonPath("$.fieldErrors.patientId").exists())
                .andExpect(jsonPath("$.fieldErrors.doctorId").exists())
                .andExpect(jsonPath("$.fieldErrors.chiefComplaint").exists())
                .andExpect(jsonPath("$.fieldErrors.diagnosis").exists());
    }

    @Test
    @DisplayName("POST /api/v1/consultations should return 409 when appointment already has consultation")
    void shouldReturn409OnDuplicateConsultation() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        RecordConsultationRequest request = new RecordConsultationRequest(
                appointmentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Follow up",
                "Hypertension",
                null,
                null
        );

        when(consultationService.recordConsultation(any(RecordConsultationRequest.class)))
                .thenThrow(new ConflictException("Consultation has already been recorded for appointment: " + appointmentId));

        mockMvc.perform(post("/api/v1/consultations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICTING_STATE"))
                .andExpect(jsonPath("$.message").value("Consultation has already been recorded for appointment: " + appointmentId));
    }

    @Test
    @DisplayName("GET /api/v1/consultations/{id} should return 200 with consultation details")
    void shouldReturnConsultationById() throws Exception {
        UUID id = UUID.randomUUID();
        UUID appointmentId = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();

        ConsultationResponse response = new ConsultationResponse(
                id,
                appointmentId,
                patientId,
                doctorId,
                "Sore throat",
                "Pharyngitis",
                "Warm saline gargles",
                null,
                Instant.now(),
                Instant.now()
        );

        when(consultationService.getConsultationById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/consultations/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.diagnosis").value("Pharyngitis"));
    }

    @Test
    @DisplayName("GET /api/v1/consultations/{id} should return 404 when consultation not found")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(consultationService.getConsultationById(id))
                .thenThrow(new ResourceNotFoundException("Consultation", id));

        mockMvc.perform(get("/api/v1/consultations/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/consultations/appointment/{appointmentId} should return 200 with consultation")
    void shouldReturnConsultationByAppointmentId() throws Exception {
        UUID appointmentId = UUID.randomUUID();
        ConsultationResponse response = new ConsultationResponse(
                UUID.randomUUID(),
                appointmentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Follow-up diabetes",
                "Type 2 Diabetes Mellitus",
                "HbA1c 6.5%",
                null,
                Instant.now(),
                Instant.now()
        );

        when(consultationService.getConsultationByAppointmentId(appointmentId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/consultations/appointment/{appointmentId}", appointmentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.appointmentId").value(appointmentId.toString()))
                .andExpect(jsonPath("$.diagnosis").value("Type 2 Diabetes Mellitus"));
    }

    @Test
    @DisplayName("GET /api/v1/consultations?patientId=... should return 200 with paginated consultations")
    void shouldReturnConsultationsByPatient() throws Exception {
        UUID patientId = UUID.randomUUID();
        ConsultationResponse response = new ConsultationResponse(
                UUID.randomUUID(),
                UUID.randomUUID(),
                patientId,
                UUID.randomUUID(),
                "Follow-up",
                "Asthma",
                null,
                null,
                Instant.now(),
                Instant.now()
        );

        when(consultationService.getConsultationsByPatientId(eq(patientId), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/consultations")
                        .param("patientId", patientId.toString())
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].patientId").value(patientId.toString()));
    }
}

package com.management.hospital.patient.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.patient.api.dto.CreatePatientRequest;
import com.management.hospital.patient.api.dto.PatientResponse;
import com.management.hospital.patient.application.PatientService;
import com.management.hospital.patient.domain.Gender;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PatientController.class)
@Import(GlobalExceptionHandler.class)
class PatientControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private PatientService patientService;

    @Test
    @DisplayName("POST /api/v1/patients should return 201 on valid patient registration")
    void shouldRegisterPatientSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        PatientResponse response = new PatientResponse(
            id,
            "MRN-20260909-000001",
            "John",
            "Doe",
            "John Doe",
            LocalDate.of(1990, 1, 1),
            Gender.MALE,
            "+1234567890",
            "john@example.com",
            "123 Main St",
            "Springfield",
            "12345",
            null, null, null,
            Instant.now()
        );

        when(patientService.registerPatient(any())).thenReturn(response);

        CreatePatientRequest request = new CreatePatientRequest(
            "John",
            "Doe",
            LocalDate.of(1990, 1, 1),
            Gender.MALE,
            "+1234567890",
            "john@example.com",
            "123 Main St",
            "Springfield",
            "12345",
            null, null, null
        );

        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.mrn").value("MRN-20260909-000001"))
            .andExpect(jsonPath("$.fullName").value("John Doe"));
    }

    @Test
    @DisplayName("POST /api/v1/patients should return 400 when validation fails")
    void shouldReturn400WhenValidationFails() throws Exception {
        CreatePatientRequest request = new CreatePatientRequest(
            "", // blank first name
            "Doe",
            LocalDate.of(1990, 1, 1),
            Gender.MALE,
            "",
            "not-an-email",
            null, null, null, null, null, null
        );

        mockMvc.perform(post("/api/v1/patients")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.firstName").exists());
    }

    @Test
    @DisplayName("GET /api/v1/patients/{id} should return 200 when patient exists")
    void shouldReturnPatientById() throws Exception {
        UUID id = UUID.randomUUID();
        PatientResponse response = new PatientResponse(
            id, "MRN-123", "Jane", "Doe", "Jane Doe",
            LocalDate.of(1992, 2, 2), Gender.FEMALE, "+1987654321", "jane@example.com",
            null, null, null, null, null, null, Instant.now()
        );

        when(patientService.getPatientById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/patients/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.firstName").value("Jane"));
    }

    @Test
    @DisplayName("GET /api/v1/patients/{id} should return 404 when patient does not exist")
    void shouldReturn404WhenPatientNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(patientService.getPatientById(id)).thenThrow(new ResourceNotFoundException("Patient", id));

        mockMvc.perform(get("/api/v1/patients/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("GET /api/v1/patients/mrn/{mrn} should return 200 when patient exists")
    void shouldReturnPatientByMrn() throws Exception {
        String mrn = "MRN-20260909-123456";
        PatientResponse response = new PatientResponse(
            UUID.randomUUID(), mrn, "Jane", "Doe", "Jane Doe",
            LocalDate.of(1992, 2, 2), Gender.FEMALE, "+1987654321", "jane@example.com",
            null, null, null, null, null, null, Instant.now()
        );

        when(patientService.getPatientByMrn(mrn)).thenReturn(response);

        mockMvc.perform(get("/api/v1/patients/mrn/{mrn}", mrn))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mrn").value(mrn))
            .andExpect(jsonPath("$.firstName").value("Jane"));
    }
}

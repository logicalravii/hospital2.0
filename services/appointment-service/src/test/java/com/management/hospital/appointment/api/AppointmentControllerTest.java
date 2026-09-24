package com.management.hospital.appointment.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.hospital.appointment.api.dto.AppointmentResponse;
import com.management.hospital.appointment.api.dto.BookAppointmentRequest;
import com.management.hospital.appointment.api.dto.CancelAppointmentRequest;
import com.management.hospital.appointment.application.AppointmentService;
import com.management.hospital.appointment.domain.AppointmentStatus;
import com.management.hospital.common.error.ResourceNotFoundException;
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
import java.time.LocalTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppointmentController.class)
@Import(GlobalExceptionHandler.class)
class AppointmentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private AppointmentService appointmentService;

    @Test
    @DisplayName("POST /api/v1/appointments should return 201 on valid appointment booking")
    void shouldBookAppointmentSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        UUID patientId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        LocalDate date = LocalDate.now().plusDays(2);
        LocalTime start = LocalTime.of(10, 0);
        LocalTime end = LocalTime.of(10, 30);

        AppointmentResponse response = new AppointmentResponse(
            id, patientId, doctorId, date, start, end,
            AppointmentStatus.SCHEDULED, "Routine checkup", null,
            Instant.now(), Instant.now()
        );

        when(appointmentService.bookAppointment(any())).thenReturn(response);

        BookAppointmentRequest request = new BookAppointmentRequest(
            patientId, doctorId, date, start, end, "Routine checkup", "GENERAL"
        );

        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.status").value("SCHEDULED"))
            .andExpect(jsonPath("$.patientId").value(patientId.toString()))
            .andExpect(jsonPath("$.doctorId").value(doctorId.toString()));
    }

    @Test
    @DisplayName("POST /api/v1/appointments should return 400 on validation failure")
    void shouldReturn400OnValidationFailure() throws Exception {
        // Missing patientId, doctorId, date in past
        String invalidJson = """
            {
              "appointmentDate": "2020-01-01",
              "startTime": "10:00:00",
              "endTime": "10:30:00"
            }
            """;

        mockMvc.perform(post("/api/v1/appointments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.patientId").exists())
            .andExpect(jsonPath("$.fieldErrors.doctorId").exists());
    }

    @Test
    @DisplayName("POST /api/v1/appointments/{id}/cancel should return 200 on cancellation")
    void shouldCancelAppointmentSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        AppointmentResponse response = new AppointmentResponse(
            id, UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1),
            LocalTime.of(9, 0), LocalTime.of(9, 30),
            AppointmentStatus.CANCELLED, "Routine checkup", "Schedule conflict",
            Instant.now(), Instant.now()
        );

        when(appointmentService.cancelAppointment(eq(id), any())).thenReturn(response);

        CancelAppointmentRequest request = new CancelAppointmentRequest("Schedule conflict");

        mockMvc.perform(post("/api/v1/appointments/{id}/cancel", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.status").value("CANCELLED"))
            .andExpect(jsonPath("$.cancellationReason").value("Schedule conflict"));
    }

    @Test
    @DisplayName("GET /api/v1/appointments/{id} should return 200 when appointment exists")
    void shouldReturnAppointmentById() throws Exception {
        UUID id = UUID.randomUUID();
        AppointmentResponse response = new AppointmentResponse(
            id, UUID.randomUUID(), UUID.randomUUID(), LocalDate.now().plusDays(1),
            LocalTime.of(9, 0), LocalTime.of(9, 30),
            AppointmentStatus.CONFIRMED, "Follow-up", null,
            Instant.now(), Instant.now()
        );

        when(appointmentService.getAppointmentById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/appointments/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("GET /api/v1/appointments/{id} should return 404 when not found")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(appointmentService.getAppointmentById(id)).thenThrow(new ResourceNotFoundException("Appointment", id));

        mockMvc.perform(get("/api/v1/appointments/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }
}

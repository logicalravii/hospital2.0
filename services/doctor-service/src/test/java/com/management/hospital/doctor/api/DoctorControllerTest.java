package com.management.hospital.doctor.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.doctor.api.dto.AvailableSlotsResponse;
import com.management.hospital.doctor.api.dto.CreateDoctorRequest;
import com.management.hospital.doctor.api.dto.CreateScheduleRequest;
import com.management.hospital.doctor.api.dto.DoctorResponse;
import com.management.hospital.doctor.api.dto.DoctorScheduleResponse;
import com.management.hospital.doctor.application.DoctorService;
import com.management.hospital.doctor.domain.Specialty;
import com.management.hospital.doctor.domain.TimeSlot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DoctorController.class)
@Import(GlobalExceptionHandler.class)
class DoctorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private DoctorService doctorService;

    @Test
    @DisplayName("POST /api/v1/doctors should return 201 on valid doctor registration")
    void shouldRegisterDoctorSuccessfully() throws Exception {
        UUID id = UUID.randomUUID();
        DoctorResponse response = new DoctorResponse(
            id,
            null,
            "LIC-9876",
            "Gregory",
            "House",
            "Gregory House",
            Specialty.GENERAL_MEDICINE,
            "house@hospital.com",
            "+16095551234",
            new BigDecimal("250.00"),
            "Suite 101",
            true,
            Instant.now()
        );

        when(doctorService.registerDoctor(any())).thenReturn(response);

        CreateDoctorRequest request = new CreateDoctorRequest(
            null,
            "LIC-9876",
            "Gregory",
            "House",
            Specialty.GENERAL_MEDICINE,
            "house@hospital.com",
            "+16095551234",
            new BigDecimal("250.00"),
            "Suite 101"
        );

        mockMvc.perform(post("/api/v1/doctors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.licenseNumber").value("LIC-9876"))
            .andExpect(jsonPath("$.fullName").value("Gregory House"))
            .andExpect(jsonPath("$.specialty").value("GENERAL_MEDICINE"));
    }

    @Test
    @DisplayName("POST /api/v1/doctors should accept case-insensitive specialty 'Cardiology'")
    void shouldAcceptCaseInsensitiveSpecialty() throws Exception {
        UUID id = UUID.randomUUID();
        DoctorResponse response = new DoctorResponse(
            id, null, "LIC-1111", "Allison", "Cameron", "Allison Cameron",
            Specialty.CARDIOLOGY, "cameron@hospital.com", "+1", BigDecimal.TEN, null, true, Instant.now()
        );

        when(doctorService.registerDoctor(any())).thenReturn(response);

        String json = """
            {
              "licenseNumber": "LIC-1111",
              "firstName": "Allison",
              "lastName": "Cameron",
              "specialty": "Cardiology",
              "email": "cameron@hospital.com",
              "phone": "+16095551111",
              "consultationFee": 150.00
            }
            """;

        mockMvc.perform(post("/api/v1/doctors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.specialty").value("CARDIOLOGY"));
    }

    @Test
    @DisplayName("POST /api/v1/doctors should return 400 when validation fails")
    void shouldReturn400OnValidationFailure() throws Exception {
        CreateDoctorRequest request = new CreateDoctorRequest(
            null,
            "",
            "",
            "House",
            Specialty.GENERAL_MEDICINE,
            "invalid-email",
            "",
            new BigDecimal("-10.00"),
            null
        );

        mockMvc.perform(post("/api/v1/doctors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.licenseNumber").exists())
            .andExpect(jsonPath("$.fieldErrors.firstName").exists())
            .andExpect(jsonPath("$.fieldErrors.email").exists());
    }

    @Test
    @DisplayName("GET /api/v1/doctors/{id} should return 200 when doctor exists")
    void shouldReturnDoctorById() throws Exception {
        UUID id = UUID.randomUUID();
        DoctorResponse response = new DoctorResponse(
            id, null, "LIC-9876", "Gregory", "House", "Gregory House",
            Specialty.GENERAL_MEDICINE, "house@hospital.com", "+1", BigDecimal.TEN, null, true, Instant.now()
        );

        when(doctorService.getDoctorById(id)).thenReturn(response);

        mockMvc.perform(get("/api/v1/doctors/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.fullName").value("Gregory House"));
    }

    @Test
    @DisplayName("GET /api/v1/doctors/{id} should return 404 when doctor does not exist")
    void shouldReturn404WhenNotFound() throws Exception {
        UUID id = UUID.randomUUID();
        when(doctorService.getDoctorById(id)).thenThrow(new ResourceNotFoundException("Doctor", id));

        mockMvc.perform(get("/api/v1/doctors/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("POST /api/v1/doctors/{id}/schedules should return 201 on valid schedule creation")
    void shouldAddScheduleSuccessfully() throws Exception {
        UUID doctorId = UUID.randomUUID();
        UUID scheduleId = UUID.randomUUID();
        DoctorScheduleResponse response = new DoctorScheduleResponse(
            scheduleId, doctorId, DayOfWeek.MONDAY, LocalTime.of(9, 0), LocalTime.of(12, 0),
            30, 6, true,
            List.of(new TimeSlot(LocalTime.of(9, 0), LocalTime.of(9, 30)))
        );

        when(doctorService.addSchedule(eq(doctorId), any())).thenReturn(response);

        CreateScheduleRequest request = new CreateScheduleRequest(
            DayOfWeek.MONDAY,
            LocalTime.of(9, 0),
            LocalTime.of(12, 0),
            30,
            6
        );

        mockMvc.perform(post("/api/v1/doctors/{id}/schedules", doctorId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.dayOfWeek").value("MONDAY"))
            .andExpect(jsonPath("$.slotDurationMinutes").value(30));
    }

    @Test
    @DisplayName("GET /api/v1/doctors/{id}/slots should return 200 with available slots")
    void shouldReturnAvailableSlots() throws Exception {
        UUID doctorId = UUID.randomUUID();
        LocalDate date = LocalDate.of(2026, 9, 21);
        AvailableSlotsResponse response = new AvailableSlotsResponse(
            doctorId,
            date,
            DayOfWeek.MONDAY,
            List.of(
                new TimeSlot(LocalTime.of(9, 0), LocalTime.of(9, 30)),
                new TimeSlot(LocalTime.of(9, 30), LocalTime.of(10, 0))
            )
        );

        when(doctorService.getAvailableSlotsForDate(doctorId, date)).thenReturn(response);

        mockMvc.perform(get("/api/v1/doctors/{id}/slots", doctorId)
                .param("date", "2026-09-21"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.doctorId").value(doctorId.toString()))
            .andExpect(jsonPath("$.slots").isArray())
            .andExpect(jsonPath("$.slots[0].startTime").value("09:00:00"));
    }
}

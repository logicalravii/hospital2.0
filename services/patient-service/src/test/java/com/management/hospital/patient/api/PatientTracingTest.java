package com.management.hospital.patient.api;

import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.common.security.SecurityHeaders;
import com.management.hospital.patient.application.PatientService;
import com.management.hospital.patient.infrastructure.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationAutoConfiguration;
import org.springframework.boot.micrometer.tracing.autoconfigure.MicrometerTracingAutoConfiguration;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.OpenTelemetryTracingAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.OpenTelemetrySdkAutoConfiguration;
import org.springframework.boot.webmvc.autoconfigure.WebMvcObservationAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies that requests are traced (with the production auto-configuration) and that
 * the trace ID reaches error responses, including requests rejected by the gateway filter.
 */
@WebMvcTest(PatientController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
@ImportAutoConfiguration({
    ObservationAutoConfiguration.class,
    OpenTelemetrySdkAutoConfiguration.class,
    MicrometerTracingAutoConfiguration.class,
    OpenTelemetryTracingAutoConfiguration.class,
    WebMvcObservationAutoConfiguration.class
})
class PatientTracingTest {

    private static final String GATEWAY_TRACE_ID = "4bf92f3577b34da6a3ce929d0e0e4736";
    private static final String GATEWAY_TRACEPARENT = "00-" + GATEWAY_TRACE_ID + "-00f067aa0ba902b7-01";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PatientService patientService;

    @Test
    @DisplayName("Error response carries the trace ID propagated from the gateway")
    void errorResponseCarriesPropagatedTraceId() throws Exception {
        UUID id = UUID.randomUUID();
        when(patientService.getPatientById(id)).thenThrow(new ResourceNotFoundException("Patient", id));

        mockMvc.perform(get("/api/v1/patients/{id}", id)
                .header("traceparent", GATEWAY_TRACEPARENT)
                .header(SecurityHeaders.USER_ID, UUID.randomUUID().toString()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.traceId").value(GATEWAY_TRACE_ID));
    }

    @Test
    @DisplayName("Direct request rejected by GatewayContextFilter is still traced and its 401 carries a traceId")
    void rejectedDirectRequestCarriesTraceId() throws Exception {
        mockMvc.perform(get("/api/v1/patients/{id}", UUID.randomUUID()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
            .andExpect(jsonPath("$.traceId").isString());
    }
}

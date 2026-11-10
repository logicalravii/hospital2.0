package com.management.hospital.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApiErrorResponseTest {

    @Test
    @DisplayName("Should create ApiErrorResponse with default timestamp and empty field errors")
    void shouldCreateBasicErrorResponse() {
        ApiErrorResponse response = new ApiErrorResponse(
            ErrorCode.RESOURCE_NOT_FOUND,
            "Patient not found",
            "/api/v1/patients/123"
        );

        assertThat(response.code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.message()).isEqualTo("Patient not found");
        assertThat(response.path()).isEqualTo("/api/v1/patients/123");
        assertThat(response.timestamp()).isNotNull();
        assertThat(response.fieldErrors()).isEmpty();
    }

    @Test
    @DisplayName("Should create ApiErrorResponse with field errors and ensure immutability")
    void shouldCreateResponseWithFieldErrors() {
        Map<String, String> errors = Map.of("email", "must be a valid email", "phone", "cannot be blank");
        ApiErrorResponse response = new ApiErrorResponse(
            ErrorCode.VALIDATION_FAILED,
            "Validation failed",
            "/api/v1/patients",
            errors
        );

        assertThat(response.fieldErrors()).hasSize(2)
            .containsEntry("email", "must be a valid email")
            .containsEntry("phone", "cannot be blank");

        assertThatThrownBy(() -> response.fieldErrors().put("newField", "error"))
            .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("Should carry the current trace ID from the logging MDC")
    void shouldCarryTraceIdFromMdc() {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(ApiErrorResponse.TRACE_ID_MDC_KEY, "4bf92f3577b34da6a3ce929d0e0e4736")) {
            ApiErrorResponse response = new ApiErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, "Patient not found", "/api/v1/patients/123");

            assertThat(response.traceId()).isEqualTo("4bf92f3577b34da6a3ce929d0e0e4736");
        }
    }

    @Test
    @DisplayName("Should omit traceId from JSON when no trace is active")
    void shouldOmitTraceIdWhenNoTraceActive() throws Exception {
        MDC.remove(ApiErrorResponse.TRACE_ID_MDC_KEY);
        ApiErrorResponse response = new ApiErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, "Patient not found", "/api/v1/patients/123");

        String json = new ObjectMapper().findAndRegisterModules().writeValueAsString(response);

        assertThat(response.traceId()).isNull();
        assertThat(json).doesNotContain("traceId");
    }
}

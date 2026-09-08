package com.management.hospital.common.error;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}

package com.management.hospital.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
    String code,
    String message,
    Instant timestamp,
    String path,
    Map<String, String> fieldErrors
) {
    public ApiErrorResponse(String code, String message, String path) {
        this(code, message, Instant.now(), path, Map.of());
    }

    public ApiErrorResponse(String code, String message, String path, Map<String, String> fieldErrors) {
        this(code, message, Instant.now(), path, fieldErrors != null ? Map.copyOf(fieldErrors) : Map.of());
    }
}

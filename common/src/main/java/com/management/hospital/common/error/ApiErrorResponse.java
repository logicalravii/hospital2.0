package com.management.hospital.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.MDC;

import java.time.Instant;
import java.util.Map;

/**
 * Standard error body returned by every service.
 *
 * <p>{@code traceId} lets a caller quote a single identifier that matches the
 * service logs for the failed request. The convenience constructors read it from
 * the logging MDC, where Micrometer Tracing places the current trace ID while a
 * request is being handled; it is omitted from the JSON when no trace is active.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
    String code,
    String message,
    Instant timestamp,
    String path,
    Map<String, String> fieldErrors,
    String traceId
) {
    /** MDC key used by Micrometer Tracing for the current trace ID. */
    public static final String TRACE_ID_MDC_KEY = "traceId";

    public ApiErrorResponse(String code, String message, String path) {
        this(code, message, Instant.now(), path, Map.of(), currentTraceId());
    }

    public ApiErrorResponse(String code, String message, String path, Map<String, String> fieldErrors) {
        this(code, message, Instant.now(), path,
            fieldErrors != null ? Map.copyOf(fieldErrors) : Map.of(), currentTraceId());
    }

    private static String currentTraceId() {
        return MDC.get(TRACE_ID_MDC_KEY);
    }
}

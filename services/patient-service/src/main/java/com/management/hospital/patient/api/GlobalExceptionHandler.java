package com.management.hospital.patient.api;

import com.management.hospital.common.error.ApiErrorResponse;
import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.DomainException;
import com.management.hospital.common.error.ErrorCode;
import com.management.hospital.common.error.ForbiddenException;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.common.error.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiErrorResponse(ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ApiErrorResponse(ErrorCode.RESOURCE_NOT_FOUND, "Path not found: " + request.getRequestURI(), request.getRequestURI()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        log.warn("Conflict detected: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(new ApiErrorResponse(ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest request) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
            .body(new ApiErrorResponse(ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest request) {
        log.warn("Forbidden operation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ApiErrorResponse(ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiErrorResponse> handleDomainException(DomainException ex, HttpServletRequest request) {
        log.warn("Domain rule violation: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ApiErrorResponse(ex.getCode(), ex.getMessage(), request.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }

        log.warn("Request validation failed: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiErrorResponse(ErrorCode.VALIDATION_FAILED, "Request validation failed", request.getRequestURI(), fieldErrors));
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Malformed HTTP request body on {}: {}", request.getRequestURI(), ex.getMessage());
        String message = "Malformed JSON request or invalid input format";

        Throwable rootCause = ex.getMostSpecificCause();
        if (rootCause instanceof tools.jackson.databind.exc.InvalidFormatException ife) {
            Class<?> targetType = ife.getTargetType();
            if (targetType != null && targetType.isEnum()) {
                String allowed = java.util.Arrays.stream(targetType.getEnumConstants())
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.joining(", "));
                message = String.format("Invalid value '%s' for %s. Allowed values are: [%s]",
                    ife.getValue(), targetType.getSimpleName(), allowed);
            } else {
                message = String.format("Invalid value '%s' for field of type %s",
                    ife.getValue(), targetType != null ? targetType.getSimpleName() : "unknown");
            }
        } else if (rootCause instanceof IllegalArgumentException iae) {
            message = iae.getMessage();
        } else if (rootCause != null && rootCause.getMessage() != null) {
            String causeMsg = rootCause.getMessage();
            if (causeMsg.contains("Cannot deserialize value of type") && causeMsg.contains("Enum class: [")) {
                int enumStart = causeMsg.indexOf("Enum class: [");
                String allowed = causeMsg.substring(enumStart + 12);
                int end = allowed.indexOf("]");
                if (end != -1) {
                    allowed = allowed.substring(0, end + 1);
                }
                message = "Invalid enum value. Allowed values are: " + allowed;
            } else {
                int locIndex = causeMsg.indexOf("\n at [Source:");
                message = (locIndex != -1) ? causeMsg.substring(0, locIndex).trim() : causeMsg;
            }
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ApiErrorResponse(ErrorCode.VALIDATION_FAILED, message, request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception occurred on {}", request.getRequestURI(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ApiErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An internal server error occurred", request.getRequestURI()));
    }
}

package com.management.hospital.common.error;

public final class ErrorCode {
    private ErrorCode() {}

    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String VALIDATION_FAILED = "VALIDATION_FAILED";
    public static final String DOMAIN_RULE_VIOLATION = "DOMAIN_RULE_VIOLATION";
    public static final String CONFLICTING_STATE = "CONFLICTING_STATE";
    public static final String UNAUTHORIZED_ACCESS = "UNAUTHORIZED_ACCESS";
    public static final String FORBIDDEN_OPERATION = "FORBIDDEN_OPERATION";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
}

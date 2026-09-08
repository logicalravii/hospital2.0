package com.management.hospital.common.error;

public class DomainException extends RuntimeException {
    private final String code;

    public DomainException(String code, String message) {
        super(message);
        this.code = code != null ? code : ErrorCode.DOMAIN_RULE_VIOLATION;
    }

    public DomainException(String message) {
        this(ErrorCode.DOMAIN_RULE_VIOLATION, message);
    }

    public String getCode() {
        return code;
    }
}

package com.management.hospital.common.error;

public class ForbiddenException extends DomainException {
    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN_OPERATION, message);
    }
}

package com.management.hospital.common.error;

public class ConflictException extends DomainException {
    public ConflictException(String message) {
        super(ErrorCode.CONFLICTING_STATE, message);
    }
}

package com.management.hospital.common.error;

public class UnauthorizedException extends DomainException {
    public UnauthorizedException(String message) {
        super(ErrorCode.UNAUTHORIZED_ACCESS, message);
    }
}

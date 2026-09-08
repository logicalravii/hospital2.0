package com.management.hospital.common.error;

public class ResourceNotFoundException extends DomainException {
    public ResourceNotFoundException(String message) {
        super(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(ErrorCode.RESOURCE_NOT_FOUND, "%s with identifier '%s' was not found".formatted(resourceName, identifier));
    }
}

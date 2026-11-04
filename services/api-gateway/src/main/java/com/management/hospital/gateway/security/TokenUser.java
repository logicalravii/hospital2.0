package com.management.hospital.gateway.security;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public record TokenUser(
    UUID userId,
    String username,
    List<String> roles
) {
    public TokenUser {
        roles = roles != null ? List.copyOf(roles) : Collections.emptyList();
    }
}

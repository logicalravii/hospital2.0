package com.management.hospital.identity.api.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record AuthResponse(
    String accessToken,
    String tokenType,
    UUID userId,
    String username,
    Set<String> roles,
    Instant expiresAt
) {
    public static AuthResponse bearer(String token, UUID userId, String username, Set<String> roles, long expirationMs) {
        return new AuthResponse(token, "Bearer", userId, username, roles, Instant.now().plusMillis(expirationMs));
    }
}

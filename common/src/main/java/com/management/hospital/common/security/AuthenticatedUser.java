package com.management.hospital.common.security;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
    UUID userId,
    String username,
    Set<String> roles
) {
    public AuthenticatedUser {
        roles = roles != null ? Set.copyOf(roles) : Collections.emptySet();
    }

    public boolean hasRole(String role) {
        if (role == null) {
            return false;
        }
        String normalized = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return roles.contains(normalized) || roles.contains(role);
    }
}

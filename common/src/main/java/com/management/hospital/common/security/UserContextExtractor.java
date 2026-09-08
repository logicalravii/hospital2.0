package com.management.hospital.common.security;

import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public final class UserContextExtractor {
    private UserContextExtractor() {}

    public static Optional<AuthenticatedUser> fromHeaders(String userIdHeader, String usernameHeader, String rolesHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            return Optional.empty();
        }

        try {
            UUID userId = UUID.fromString(userIdHeader.trim());
            String username = (usernameHeader != null && !usernameHeader.isBlank()) ? usernameHeader.trim() : "anonymous";
            Set<String> roles = Collections.emptySet();

            if (rolesHeader != null && !rolesHeader.isBlank()) {
                roles = Arrays.stream(rolesHeader.split(","))
                    .map(String::trim)
                    .filter(r -> !r.isEmpty())
                    .collect(Collectors.toUnmodifiableSet());
            }

            return Optional.of(new AuthenticatedUser(userId, username, roles));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

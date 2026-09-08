package com.management.hospital.common.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserContextExtractorTest {

    @Test
    @DisplayName("Should parse valid headers into AuthenticatedUser")
    void shouldParseValidHeaders() {
        UUID userId = UUID.randomUUID();
        String roles = "ROLE_DOCTOR, ROLE_ADMIN";

        Optional<AuthenticatedUser> userOpt = UserContextExtractor.fromHeaders(
            userId.toString(),
            "dr_smith",
            roles
        );

        assertThat(userOpt).isPresent();
        AuthenticatedUser user = userOpt.get();
        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.username()).isEqualTo("dr_smith");
        assertThat(user.roles()).containsExactlyInAnyOrder("ROLE_DOCTOR", "ROLE_ADMIN");
        assertThat(user.hasRole("DOCTOR")).isTrue();
        assertThat(user.hasRole("ROLE_DOCTOR")).isTrue();
        assertThat(user.hasRole("RECEPTIONIST")).isFalse();
    }

    @Test
    @DisplayName("Should return empty when userId header is null or blank")
    void shouldReturnEmptyForMissingUserId() {
        assertThat(UserContextExtractor.fromHeaders(null, "user", "ROLE_ADMIN")).isEmpty();
        assertThat(UserContextExtractor.fromHeaders("   ", "user", "ROLE_ADMIN")).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when userId header is not a valid UUID")
    void shouldReturnEmptyForInvalidUUID() {
        assertThat(UserContextExtractor.fromHeaders("invalid-uuid", "user", "ROLE_ADMIN")).isEmpty();
    }

    @Test
    @DisplayName("Should handle missing roles gracefully")
    void shouldHandleMissingRoles() {
        UUID userId = UUID.randomUUID();
        Optional<AuthenticatedUser> userOpt = UserContextExtractor.fromHeaders(
            userId.toString(),
            "plain_user",
            null
        );

        assertThat(userOpt).isPresent();
        assertThat(userOpt.get().roles()).isEmpty();
        assertThat(userOpt.get().hasRole("ADMIN")).isFalse();
    }
}

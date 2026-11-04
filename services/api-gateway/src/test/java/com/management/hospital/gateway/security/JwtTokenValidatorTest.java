package com.management.hospital.gateway.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenValidatorTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private JwtTokenValidator validator;
    private SecretKey signingKey;

    @BeforeEach
    void setUp() {
        this.validator = new JwtTokenValidator(TEST_SECRET);
        this.signingKey = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String createToken(UUID userId, String username, List<String> roles, Instant expiry) {
        return Jwts.builder()
            .subject(username)
            .claim("userId", userId != null ? userId.toString() : null)
            .claim("roles", roles)
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(expiry))
            .signWith(signingKey)
            .compact();
    }

    @Test
    @DisplayName("Should successfully validate token and extract user details")
    void shouldValidateAndExtractUserDetails() {
        UUID userId = UUID.randomUUID();
        String username = "doctor.smith";
        List<String> roles = List.of("ROLE_DOCTOR", "ROLE_STAFF");
        Instant expiry = Instant.now().plus(1, ChronoUnit.HOURS);

        String token = createToken(userId, username, roles, expiry);

        Optional<TokenUser> result = validator.validateAndExtract(token);

        assertThat(result).isPresent();
        TokenUser user = result.get();
        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.username()).isEqualTo(username);
        assertThat(user.roles()).containsExactlyInAnyOrder("ROLE_DOCTOR", "ROLE_STAFF");
    }

    @Test
    @DisplayName("Should return empty when token is expired")
    void shouldReturnEmptyForExpiredToken() {
        UUID userId = UUID.randomUUID();
        Instant expiredAt = Instant.now().minus(1, ChronoUnit.HOURS);

        String token = createToken(userId, "expired.user", List.of("ROLE_NURSE"), expiredAt);

        Optional<TokenUser> result = validator.validateAndExtract(token);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when token signature is invalid / tampered")
    void shouldReturnEmptyForTamperedToken() {
        SecretKey otherKey = Keys.hmacShaKeyFor("different-secret-key-that-is-at-least-256-bits-long-32-bytes!!".getBytes(StandardCharsets.UTF_8));
        String tamperedToken = Jwts.builder()
            .subject("hacker")
            .claim("userId", UUID.randomUUID().toString())
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(otherKey)
            .compact();

        Optional<TokenUser> result = validator.validateAndExtract(tamperedToken);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when token is null or blank")
    void shouldReturnEmptyForNullOrBlank() {
        assertThat(validator.validateAndExtract(null)).isEmpty();
        assertThat(validator.validateAndExtract("")).isEmpty();
        assertThat(validator.validateAndExtract("   ")).isEmpty();
    }

    @Test
    @DisplayName("Should return empty when userId claim is missing or malformed")
    void shouldReturnEmptyForInvalidUserId() {
        String tokenNoUserId = Jwts.builder()
            .subject("valid.user")
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(signingKey)
            .compact();

        assertThat(validator.validateAndExtract(tokenNoUserId)).isEmpty();

        String tokenMalformedUserId = Jwts.builder()
            .subject("valid.user")
            .claim("userId", "not-a-valid-uuid")
            .expiration(Date.from(Instant.now().plus(1, ChronoUnit.HOURS)))
            .signWith(signingKey)
            .compact();

        assertThat(validator.validateAndExtract(tokenMalformedUserId)).isEmpty();
    }
}

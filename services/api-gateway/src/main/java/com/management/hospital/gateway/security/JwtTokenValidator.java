package com.management.hospital.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JwtTokenValidator {

    private final SecretKey key;

    public JwtTokenValidator(@Value("${hms.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<TokenUser> validateAndExtract(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token.trim())
                .getPayload();

            String username = claims.getSubject();
            if (username == null || username.isBlank()) {
                return Optional.empty();
            }

            String userIdStr = claims.get("userId", String.class);
            UUID userId;
            try {
                userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }

            if (userId == null) {
                return Optional.empty();
            }

            List<?> rawRoles = claims.get("roles", List.class);
            List<String> roles = (rawRoles != null)
                ? rawRoles.stream().map(Object::toString).toList()
                : Collections.emptyList();

            return Optional.of(new TokenUser(userId, username, roles));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}

package com.management.hospital.gateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.management.hospital.common.security.SecurityHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtAuthenticationFilterTest {

    private JwtTokenValidator validator;
    private JwtAuthenticationFilter filter;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = mock(JwtTokenValidator.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        filter = new JwtAuthenticationFilter(validator, objectMapper);
    }

    @Test
    @DisplayName("Open path: should allow request without token and strip spoofed headers")
    void openPathShouldAllowWithoutTokenAndSanitizeSpoofedHeaders() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login")
            .header(SecurityHeaders.USER_ID, UUID.randomUUID().toString())
            .header(SecurityHeaders.USER_ROLES, "ROLE_ADMIN")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();

        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            downstreamExchange.set(ex);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(SecurityHeaders.USER_ID)).isNull();
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(SecurityHeaders.USER_ROLES)).isNull();
    }

    @Test
    @DisplayName("Open path: should propagate user headers when valid token is provided")
    void openPathShouldPropagateHeadersWhenTokenPresent() {
        UUID userId = UUID.randomUUID();
        when(validator.validateAndExtract("valid-token"))
            .thenReturn(Optional.of(new TokenUser(userId, "admin", List.of("ROLE_ADMIN"))));

        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/register")
            .header(HttpHeaders.AUTHORIZATION, "Bearer valid-token")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            downstreamExchange.set(ex);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(SecurityHeaders.USER_ID))
            .isEqualTo(userId.toString());
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(SecurityHeaders.USER_NAME))
            .isEqualTo("admin");
        assertThat(downstreamExchange.get().getRequest().getHeaders().getFirst(SecurityHeaders.USER_ROLES))
            .isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Protected path: should reject request with 401 when Authorization header is missing")
    void protectedPathShouldRejectWhenAuthHeaderMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/patients")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Protected path: should reject request with 401 when Authorization header is not Bearer")
    void protectedPathShouldRejectWhenNotBearer() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/patients/123")
            .header(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNz")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Protected path: should reject request with 401 when token is invalid or expired")
    void protectedPathShouldRejectWhenTokenInvalid() {
        when(validator.validateAndExtract(anyString())).thenReturn(Optional.empty());

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/doctors")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-or-expired-token")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicBoolean chainCalled = new AtomicBoolean(false);
        GatewayFilterChain chain = ex -> {
            chainCalled.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Protected path: should propagate user context headers when token is valid")
    void protectedPathShouldPropagateHeadersWhenTokenValid() {
        UUID userId = UUID.randomUUID();
        when(validator.validateAndExtract("valid-jwt"))
            .thenReturn(Optional.of(new TokenUser(userId, "doctor.who", List.of("ROLE_DOCTOR", "ROLE_USER"))));

        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/appointments")
            .header(HttpHeaders.AUTHORIZATION, "Bearer valid-jwt")
            .header(SecurityHeaders.USER_ID, "spoofed-user-id")
            .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        AtomicReference<ServerWebExchange> downstreamExchange = new AtomicReference<>();
        GatewayFilterChain chain = ex -> {
            downstreamExchange.set(ex);
            return Mono.empty();
        };

        StepVerifier.create(filter.filter(exchange, chain))
            .verifyComplete();

        ServerHttpRequest downstream = downstreamExchange.get().getRequest();
        assertThat(downstream.getHeaders().getFirst(SecurityHeaders.USER_ID)).isEqualTo(userId.toString());
        assertThat(downstream.getHeaders().getFirst(SecurityHeaders.USER_NAME)).isEqualTo("doctor.who");
        assertThat(downstream.getHeaders().getFirst(SecurityHeaders.USER_ROLES)).isEqualTo("ROLE_DOCTOR,ROLE_USER");
    }
}

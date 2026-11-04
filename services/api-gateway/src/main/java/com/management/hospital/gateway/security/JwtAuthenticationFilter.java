package com.management.hospital.gateway.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.hospital.common.error.ApiErrorResponse;
import com.management.hospital.common.security.SecurityHeaders;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final List<String> OPEN_PATH_PREFIXES = List.of(
        "/api/v1/auth",
        "/actuator",
        "/favicon.ico"
    );

    private final JwtTokenValidator jwtTokenValidator;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenValidator jwtTokenValidator, ObjectMapper objectMapper) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (isOpenPath(path)) {
            // For open paths, if a valid token is provided, propagate headers; otherwise sanitize spoofed headers
            ServerHttpRequest.Builder builder = request.mutate();
            sanitizeHeaders(builder);

            if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
                String token = authHeader.substring(BEARER_PREFIX.length()).trim();
                jwtTokenValidator.validateAndExtract(token).ifPresent(user -> populateHeaders(builder, user));
            }
            return chain.filter(exchange.mutate().request(builder.build()).build());
        }

        // Protected endpoint validation
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return writeErrorResponse(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Missing or invalid authorization token"
            );
        }

        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        Optional<TokenUser> userOpt = jwtTokenValidator.validateAndExtract(token);

        if (userOpt.isEmpty()) {
            return writeErrorResponse(
                exchange,
                HttpStatus.UNAUTHORIZED,
                "UNAUTHORIZED",
                "Invalid or expired authorization token"
            );
        }

        TokenUser user = userOpt.get();
        ServerHttpRequest.Builder builder = request.mutate();
        sanitizeHeaders(builder);
        populateHeaders(builder, user);

        return chain.filter(exchange.mutate().request(builder.build()).build());
    }

    private boolean isOpenPath(String path) {
        return OPEN_PATH_PREFIXES.stream().anyMatch(path::startsWith);
    }

    private void sanitizeHeaders(ServerHttpRequest.Builder builder) {
        builder.headers(headers -> {
            headers.remove(SecurityHeaders.USER_ID);
            headers.remove(SecurityHeaders.USER_ROLES);
            headers.remove(SecurityHeaders.USER_NAME);
        });
    }

    private void populateHeaders(ServerHttpRequest.Builder builder, TokenUser user) {
        builder.header(SecurityHeaders.USER_ID, user.userId().toString());
        builder.header(SecurityHeaders.USER_NAME, user.username());
        if (!user.roles().isEmpty()) {
            builder.header(SecurityHeaders.USER_ROLES, String.join(",", user.roles()));
        }
    }

    private Mono<Void> writeErrorResponse(ServerWebExchange exchange, HttpStatus status, String code, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiErrorResponse errorResponse = new ApiErrorResponse(
            code,
            message,
            exchange.getRequest().getPath().value()
        );

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorResponse);
        } catch (JsonProcessingException e) {
            bytes = ("{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}

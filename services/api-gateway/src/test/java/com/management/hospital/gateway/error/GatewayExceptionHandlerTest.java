package com.management.hospital.gateway.error;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayExceptionHandlerTest {

    private GatewayExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        exceptionHandler = new GatewayExceptionHandler(objectMapper);
    }

    @Test
    @DisplayName("Should handle ResponseStatusException 404 NOT FOUND")
    void shouldHandleResponseStatusException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/unknown").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "Resource not mapped");

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("Should handle ConnectException with 503 SERVICE UNAVAILABLE")
    void shouldHandleConnectException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/patients").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        ConnectException ex = new ConnectException("Connection refused");

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    @DisplayName("Should handle generic unexpected runtime exception with 500 INTERNAL SERVER ERROR")
    void shouldHandleUnexpectedException() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/doctors").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        RuntimeException ex = new RuntimeException("Unexpected gateway glitch");

        StepVerifier.create(exceptionHandler.handle(exchange, ex))
            .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}

package com.management.hospital.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToServer()
            .baseUrl("http://localhost:" + port)
            .build();
    }

    @Test
    @DisplayName("Unauthenticated request to protected endpoint /api/v1/patients returns 401 UNAUTHORIZED")
    void unauthenticatedCallToPatientsReturns401() {
        webTestClient.get()
            .uri("/api/v1/patients")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
            .jsonPath("$.message").isEqualTo("Missing or invalid authorization token")
            .jsonPath("$.path").isEqualTo("/api/v1/patients");
    }

    @Test
    @DisplayName("Request with invalid token to /api/v1/doctors returns 401 UNAUTHORIZED")
    void malformedTokenReturns401() {
        webTestClient.get()
            .uri("/api/v1/doctors")
            .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-jwt-token")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("UNAUTHORIZED")
            .jsonPath("$.message").isEqualTo("Invalid or expired authorization token")
            .jsonPath("$.path").isEqualTo("/api/v1/doctors");
    }

    @Test
    @DisplayName("Request with invalid token to /api/v1/appointments returns 401 UNAUTHORIZED")
    void appointmentEndpointUnauthorizedWithoutToken() {
        webTestClient.get()
            .uri("/api/v1/appointments")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }

    @Test
    @DisplayName("Request with invalid token to /api/v1/consultations returns 401 UNAUTHORIZED")
    void consultationEndpointUnauthorizedWithoutToken() {
        webTestClient.get()
            .uri("/api/v1/consultations")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.code").isEqualTo("UNAUTHORIZED");
    }
}

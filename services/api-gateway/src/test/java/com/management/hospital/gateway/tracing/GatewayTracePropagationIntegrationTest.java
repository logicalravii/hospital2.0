package com.management.hospital.gateway.tracing;

import com.sun.net.httpserver.HttpServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies end-to-end trace propagation through the gateway against a stub downstream service.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayTracePropagationIntegrationTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final String TRACEPARENT = "traceparent";

    private static final AtomicReference<String> downstreamTraceParent = new AtomicReference<>();
    private static final HttpServer downstream = startStubDownstream();
    private static final int unreachablePort = findFreePort();

    @LocalServerPort
    private int port;

    private WebTestClient client;

    @DynamicPropertySource
    static void routes(DynamicPropertyRegistry registry) {
        registry.add("SERVICES_IDENTITY_URL", () -> "http://localhost:" + downstream.getAddress().getPort());
        registry.add("SERVICES_DOCTOR_URL", () -> "http://localhost:" + unreachablePort);
    }

    @AfterAll
    static void stopDownstream() {
        downstream.stop(0);
    }

    @BeforeEach
    void setUp() {
        downstreamTraceParent.set(null);
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    @DisplayName("Routed request: downstream receives a traceparent whose trace ID matches the X-Trace-Id response header")
    void forwardsTraceContextToDownstream() {
        EntityExchangeResult<byte[]> result = client.post().uri("/api/v1/auth/login")
            .exchange()
            .expectStatus().isOk()
            .expectBody().returnResult();

        String traceId = result.getResponseHeaders().getFirst(TracePropagationFilter.TRACE_ID_HEADER);
        assertThat(traceId).matches("[0-9a-f]{32}");
        assertThat(downstreamTraceParent.get()).startsWith("00-" + traceId + "-");
    }

    @Test
    @DisplayName("Incoming traceparent from the caller is continued, with the gateway's span as the new parent")
    void continuesIncomingTrace() {
        String callerTraceId = "4bf92f3577b34da6a3ce929d0e0e4736";
        String callerSpanId = "00f067aa0ba902b7";

        EntityExchangeResult<byte[]> result = client.post().uri("/api/v1/auth/login")
            .header(TRACEPARENT, "00-" + callerTraceId + "-" + callerSpanId + "-01")
            .exchange()
            .expectStatus().isOk()
            .expectBody().returnResult();

        assertThat(result.getResponseHeaders().getFirst(TracePropagationFilter.TRACE_ID_HEADER)).isEqualTo(callerTraceId);
        assertThat(downstreamTraceParent.get())
            .startsWith("00-" + callerTraceId + "-")
            .doesNotContain(callerSpanId);
    }

    @Test
    @DisplayName("Gateway 401 carries the same traceId in the header and the error body")
    void unauthorizedResponseCarriesTraceId() {
        EntityExchangeResult<byte[]> result = client.get().uri("/api/v1/patients")
            .exchange()
            .expectStatus().isUnauthorized()
            .expectBody().returnResult();

        String traceId = result.getResponseHeaders().getFirst(TracePropagationFilter.TRACE_ID_HEADER);
        assertThat(traceId).matches("[0-9a-f]{32}");
        assertThat(new String(result.getResponseBody(), StandardCharsets.UTF_8))
            .contains("\"traceId\":\"" + traceId + "\"");
    }

    @Test
    @DisplayName("Downstream unreachable: 503 error body carries the request's traceId")
    void serviceUnavailableResponseCarriesTraceId() {
        EntityExchangeResult<byte[]> result = client.get().uri("/api/v1/doctors")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken())
            .exchange()
            .expectStatus().isEqualTo(503)
            .expectBody().returnResult();

        String traceId = result.getResponseHeaders().getFirst(TracePropagationFilter.TRACE_ID_HEADER);
        assertThat(traceId).matches("[0-9a-f]{32}");
        assertThat(new String(result.getResponseBody(), StandardCharsets.UTF_8))
            .contains("\"code\":\"SERVICE_UNAVAILABLE\"")
            .contains("\"traceId\":\"" + traceId + "\"");
    }

    private static String validToken() {
        return Jwts.builder()
            .subject("dr.smith")
            .claim("userId", UUID.randomUUID().toString())
            .claim("roles", List.of("ROLE_DOCTOR"))
            .issuedAt(Date.from(Instant.now()))
            .expiration(Date.from(Instant.now().plusSeconds(300)))
            .signWith(Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8)))
            .compact();
    }

    private static HttpServer startStubDownstream() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                downstreamTraceParent.set(exchange.getRequestHeaders().getFirst(TRACEPARENT));
                byte[] body = "{}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException("Could not start stub downstream service", e);
        }
    }

    private static int findFreePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}

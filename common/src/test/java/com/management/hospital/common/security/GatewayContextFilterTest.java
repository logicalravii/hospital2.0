package com.management.hospital.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class GatewayContextFilterTest {

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    private final GatewayContextFilter filter = new GatewayContextFilter(objectMapper);

    @Test
    @DisplayName("Should reject request without gateway user context with 401")
    void shouldRejectRequestWithoutGatewayContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/appointments");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("UNAUTHORIZED");
        assertThat(response.getContentAsString()).contains("Direct access prohibited");
        verifyNoInteractions(chain);
    }

    @Test
    @DisplayName("Should allow request with valid gateway user context")
    void shouldAllowRequestWithValidGatewayContext() throws Exception {
        UUID userId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/appointments");
        request.addHeader(SecurityHeaders.USER_ID, userId.toString());
        request.addHeader(SecurityHeaders.USER_NAME, "dr.smith");
        request.addHeader(SecurityHeaders.USER_ROLES, "DOCTOR,ADMIN");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        verify(chain).doFilter(request, response);
        AuthenticatedUser user = (AuthenticatedUser) request.getAttribute("AUTHENTICATED_USER");
        assertThat(user).isNotNull();
        assertThat(user.userId()).isEqualTo(userId);
        assertThat(user.username()).isEqualTo("dr.smith");
        assertThat(user.roles()).containsExactlyInAnyOrder("DOCTOR", "ADMIN");
    }

    @Test
    @DisplayName("Should bypass actuator endpoints")
    void shouldBypassActuatorEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}


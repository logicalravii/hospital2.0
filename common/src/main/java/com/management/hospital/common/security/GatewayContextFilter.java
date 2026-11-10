package com.management.hospital.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.hospital.common.error.ApiErrorResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Enforces that incoming requests have been authenticated and routed through the API Gateway.
 * If the gateway security context (X-User-Id header) is missing on protected routes,
 * the request is rejected with HTTP 401 Unauthorized.
 */
public class GatewayContextFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    public GatewayContextFilter() {
        this(new ObjectMapper().findAndRegisterModules());
    }

    public GatewayContextFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper().findAndRegisterModules();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Allow actuator health checks or error dispatches
        if (path.startsWith("/actuator") || path.startsWith("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader(SecurityHeaders.USER_ID);
        String username = request.getHeader(SecurityHeaders.USER_NAME);
        String roles = request.getHeader(SecurityHeaders.USER_ROLES);

        Optional<AuthenticatedUser> userContext = UserContextExtractor.fromHeaders(userId, username, roles);

        if (userContext.isEmpty()) {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiErrorResponse error = new ApiErrorResponse(
                    "UNAUTHORIZED",
                    "Direct access prohibited: missing authenticated user context from API Gateway",
                    path
            );
            response.getWriter().write(objectMapper.writeValueAsString(error));
            return;
        }

        request.setAttribute("AUTHENTICATED_USER", userContext.get());

        filterChain.doFilter(request, response);
    }
}


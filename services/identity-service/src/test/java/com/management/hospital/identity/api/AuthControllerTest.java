package com.management.hospital.identity.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.management.hospital.common.error.UnauthorizedException;
import com.management.hospital.identity.api.dto.AuthResponse;
import com.management.hospital.identity.api.dto.LoginRequest;
import com.management.hospital.identity.api.dto.RegisterRequest;
import com.management.hospital.identity.api.dto.UserResponse;
import com.management.hospital.identity.application.AuthenticationService;
import com.management.hospital.identity.domain.Role;
import com.management.hospital.identity.infrastructure.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private AuthenticationService authService;

    @Test
    @DisplayName("POST /api/v1/auth/login should return 200 and token on valid credentials")
    void shouldReturnTokenOnValidLogin() throws Exception {
        AuthResponse authResponse = new AuthResponse("sample.jwt.token", "Bearer", UUID.randomUUID(), "testuser", Set.of("ROLE_ADMIN"), Instant.now().plusSeconds(3600));
        when(authService.login(any())).thenReturn(authResponse);

        LoginRequest request = new LoginRequest("testuser", "password123");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("sample.jwt.token"))
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 401 on invalid credentials")
    void shouldReturn401OnInvalidLogin() throws Exception {
        when(authService.login(any())).thenThrow(new UnauthorizedException("Invalid credentials provided"));

        LoginRequest request = new LoginRequest("testuser", "wrongpwd");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("UNAUTHORIZED_ACCESS"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login should return 400 when body validation fails")
    void shouldReturn400OnBlankPassword() throws Exception {
        LoginRequest request = new LoginRequest("testuser", "");

        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
            .andExpect(jsonPath("$.fieldErrors.password").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/register should return 201 on valid registration")
    void shouldReturn201OnRegistration() throws Exception {
        UUID newId = UUID.randomUUID();
        UserResponse userResponse = new UserResponse(newId, "newuser", "newuser@hospital.com", true, Set.of(Role.DOCTOR));
        when(authService.register(any())).thenReturn(userResponse);

        RegisterRequest request = new RegisterRequest("newuser", "newuser@hospital.com", "password123", Set.of(Role.DOCTOR));

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("newuser"))
            .andExpect(jsonPath("$.email").value("newuser@hospital.com"));
    }
}

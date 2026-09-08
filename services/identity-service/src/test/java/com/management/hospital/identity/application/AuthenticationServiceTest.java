package com.management.hospital.identity.application;

import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.UnauthorizedException;
import com.management.hospital.identity.api.dto.AuthResponse;
import com.management.hospital.identity.api.dto.LoginRequest;
import com.management.hospital.identity.api.dto.RegisterRequest;
import com.management.hospital.identity.api.dto.UserResponse;
import com.management.hospital.identity.domain.Role;
import com.management.hospital.identity.domain.UserAccount;
import com.management.hospital.identity.infrastructure.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private UserAccountRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider tokenProvider;

    private AuthenticationService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthenticationService(userRepository, passwordEncoder, tokenProvider);
    }

    @Test
    @DisplayName("Should successfully authenticate and return token when credentials are valid")
    void shouldLoginSuccessfully() {
        UserAccount user = new UserAccount("doctor_jane", "jane@hospital.com", "hashed_pwd", Set.of(Role.DOCTOR));
        when(userRepository.findByUsername("doctor_jane")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("raw_pwd", "hashed_pwd")).thenReturn(true);
        when(tokenProvider.generateToken(any(), anyString(), any())).thenReturn("mock.jwt.token");
        when(tokenProvider.getExpirationMs()).thenReturn(86400000L);

        AuthResponse response = authService.login(new LoginRequest("doctor_jane", "raw_pwd"));

        assertThat(response.accessToken()).isEqualTo("mock.jwt.token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.username()).isEqualTo("doctor_jane");
        assertThat(response.roles()).contains("ROLE_DOCTOR");
    }

    @Test
    @DisplayName("Should reject login when password does not match")
    void shouldRejectInvalidPassword() {
        UserAccount user = new UserAccount("admin", "admin@hospital.com", "hashed_pwd", Set.of(Role.ADMIN));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong_pwd", "hashed_pwd")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong_pwd")))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("Invalid credentials");
    }

    @Test
    @DisplayName("Should reject login when user account is inactive")
    void shouldRejectInactiveUser() {
        UserAccount user = new UserAccount("inactive_user", "inactive@hospital.com", "hashed_pwd", Set.of(Role.NURSE));
        user.deactivate();
        when(userRepository.findByUsername("inactive_user")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(new LoginRequest("inactive_user", "pwd")))
            .isInstanceOf(UnauthorizedException.class)
            .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("Should successfully register a new staff user")
    void shouldRegisterNewUser() {
        RegisterRequest request = new RegisterRequest("new_doctor", "new_doc@hospital.com", "secret123", Set.of(Role.DOCTOR));
        when(userRepository.existsByUsername("new_doctor")).thenReturn(false);
        when(userRepository.existsByEmail("new_doc@hospital.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded_secret");

        UserAccount savedUser = new UserAccount("new_doctor", "new_doc@hospital.com", "encoded_secret", Set.of(Role.DOCTOR));
        when(userRepository.save(any(UserAccount.class))).thenReturn(savedUser);

        UserResponse response = authService.register(request);

        assertThat(response.username()).isEqualTo("new_doctor");
        assertThat(response.email()).isEqualTo("new_doc@hospital.com");
        assertThat(response.roles()).contains(Role.DOCTOR);
    }

    @Test
    @DisplayName("Should throw ConflictException when username or email already exists")
    void shouldRejectDuplicateRegistration() {
        RegisterRequest request = new RegisterRequest("existing", "existing@hospital.com", "secret123", Set.of(Role.ADMIN));
        when(userRepository.existsByUsername("existing")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("already registered");
    }
}

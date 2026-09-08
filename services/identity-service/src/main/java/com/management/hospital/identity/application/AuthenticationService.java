package com.management.hospital.identity.application;

import com.management.hospital.common.error.ConflictException;
import com.management.hospital.common.error.ResourceNotFoundException;
import com.management.hospital.common.error.UnauthorizedException;
import com.management.hospital.identity.api.dto.AuthResponse;
import com.management.hospital.identity.api.dto.LoginRequest;
import com.management.hospital.identity.api.dto.RegisterRequest;
import com.management.hospital.identity.api.dto.UserResponse;
import com.management.hospital.identity.domain.Role;
import com.management.hospital.identity.domain.UserAccount;
import com.management.hospital.identity.infrastructure.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthenticationService {

    private final UserAccountRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthenticationService(
        UserAccountRepository userRepository,
        PasswordEncoder passwordEncoder,
        JwtTokenProvider tokenProvider
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String identifier = request.usernameOrEmail().trim().toLowerCase();

        UserAccount user = userRepository.findByUsername(identifier)
            .or(() -> userRepository.findByEmail(identifier))
            .orElseThrow(() -> new UnauthorizedException("Invalid credentials provided"));

        if (!user.isActive()) {
            throw new UnauthorizedException("User account is inactive");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials provided");
        }

        String token = tokenProvider.generateToken(user.getId(), user.getUsername(), user.getRoles());
        Set<String> roleAuthorities = user.getRoles().stream()
            .map(Role::toAuthority)
            .collect(Collectors.toUnmodifiableSet());

        return AuthResponse.bearer(
            token,
            user.getId(),
            user.getUsername(),
            roleAuthorities,
            tokenProvider.getExpirationMs()
        );
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        String username = request.username().trim().toLowerCase();
        String email = request.email().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new ConflictException("Username '%s' is already registered".formatted(username));
        }
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email '%s' is already registered".formatted(email));
        }

        String hashedPassword = passwordEncoder.encode(request.password());
        UserAccount user = new UserAccount(username, email, hashedPassword, request.roles());
        UserAccount saved = userRepository.save(user);

        return new UserResponse(
            saved.getId(),
            saved.getUsername(),
            saved.getEmail(),
            saved.isActive(),
            saved.getRoles()
        );
    }

    @Transactional(readOnly = true)
    public UserResponse getUserById(UUID userId) {
        UserAccount user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("UserAccount", userId));

        return new UserResponse(
            user.getId(),
            user.getUsername(),
            user.getEmail(),
            user.isActive(),
            user.getRoles()
        );
    }
}

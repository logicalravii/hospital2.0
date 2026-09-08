package com.management.hospital.identity.api.dto;

import com.management.hospital.identity.domain.Role;
import java.util.Set;
import java.util.UUID;

public record UserResponse(
    UUID id,
    String username,
    String email,
    boolean active,
    Set<Role> roles
) {}

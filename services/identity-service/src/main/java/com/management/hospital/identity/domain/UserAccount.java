package com.management.hospital.identity.domain;

import com.management.hospital.common.error.DomainException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false)
    private boolean active;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false)
    @Enumerated(EnumType.STRING)
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected UserAccount() {
        // JPA required
    }

    public UserAccount(String username, String email, String passwordHash, Set<Role> roles) {
        if (username == null || username.isBlank()) {
            throw new DomainException("Username cannot be blank");
        }
        if (email == null || email.isBlank()) {
            throw new DomainException("Email cannot be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new DomainException("Password hash cannot be blank");
        }
        if (roles == null || roles.isEmpty()) {
            throw new DomainException("User must have at least one role");
        }

        this.username = username.trim().toLowerCase();
        this.email = email.trim().toLowerCase();
        this.passwordHash = passwordHash;
        this.active = true;
        this.roles.addAll(roles);
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Role> getRoles() {
        return Collections.unmodifiableSet(roles);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        this.active = true;
        this.updatedAt = Instant.now();
    }

    public void addRole(Role role) {
        if (role == null) {
            throw new DomainException("Cannot add a null role");
        }
        this.roles.add(role);
        this.updatedAt = Instant.now();
    }

    public void removeRole(Role role) {
        if (this.roles.size() <= 1 && this.roles.contains(role)) {
            throw new DomainException("Cannot remove the last remaining role");
        }
        this.roles.remove(role);
        this.updatedAt = Instant.now();
    }
}

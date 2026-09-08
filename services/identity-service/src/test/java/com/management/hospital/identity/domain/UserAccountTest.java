package com.management.hospital.identity.domain;

import com.management.hospital.common.error.DomainException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserAccountTest {

    @Test
    @DisplayName("Should successfully instantiate UserAccount with valid invariants")
    void shouldCreateUserAccount() {
        UserAccount user = new UserAccount("admin", "admin@hospital.com", "hashed_pw", Set.of(Role.ADMIN));

        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getEmail()).isEqualTo("admin@hospital.com");
        assertThat(user.getPasswordHash()).isEqualTo("hashed_pw");
        assertThat(user.isActive()).isTrue();
        assertThat(user.getRoles()).containsExactly(Role.ADMIN);
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should fail when creating UserAccount without roles")
    void shouldFailWhenRolesEmpty() {
        assertThatThrownBy(() -> new UserAccount("doctor1", "doc@hospital.com", "hashed_pw", Set.of()))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("User must have at least one role");
    }

    @Test
    @DisplayName("Should fail when creating UserAccount with blank username or email")
    void shouldFailWhenBlankCredentials() {
        assertThatThrownBy(() -> new UserAccount("   ", "doc@hospital.com", "hashed_pw", Set.of(Role.DOCTOR)))
            .isInstanceOf(DomainException.class);

        assertThatThrownBy(() -> new UserAccount("doc", "   ", "hashed_pw", Set.of(Role.DOCTOR)))
            .isInstanceOf(DomainException.class);
    }

    @Test
    @DisplayName("Should manage roles and prevent removing the last role")
    void shouldManageRoles() {
        UserAccount user = new UserAccount("nurse1", "nurse@hospital.com", "hashed_pw", Set.of(Role.NURSE));

        user.addRole(Role.ADMIN);
        assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.NURSE, Role.ADMIN);

        user.removeRole(Role.ADMIN);
        assertThat(user.getRoles()).containsExactly(Role.NURSE);

        assertThatThrownBy(() -> user.removeRole(Role.NURSE))
            .isInstanceOf(DomainException.class)
            .hasMessageContaining("Cannot remove the last remaining role");
    }

    @Test
    @DisplayName("Should deactivate and activate user")
    void shouldToggleActiveStatus() {
        UserAccount user = new UserAccount("user1", "user1@hospital.com", "hashed_pw", Set.of(Role.RECEPTIONIST));
        assertThat(user.isActive()).isTrue();

        user.deactivate();
        assertThat(user.isActive()).isFalse();

        user.activate();
        assertThat(user.isActive()).isTrue();
    }
}

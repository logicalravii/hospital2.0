# Testing Skill

## Purpose

Create tests that verify behavior rather than implementation details.

## Test Layers

Prefer:

Unit Tests
    ↓
Integration Tests
    ↓
API Tests

## Unit Tests

Use JUnit 5.

Test:

- domain rules
- state transitions
- edge cases
- validation
- application behavior

Use Mockito only when useful.

## Integration Tests

Use:

- Spring Boot Test
- Testcontainers
- PostgreSQL container

Verify:

- JPA mappings
- repository queries
- transactions
- database constraints
- REST integration

## Rules

Tests must be:

- deterministic
- readable
- isolated
- meaningful

Do not write tests only to increase coverage percentage.

A test should fail when meaningful behavior breaks.

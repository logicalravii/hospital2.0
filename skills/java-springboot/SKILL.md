# Java Spring Boot Skill

## Purpose

Implement maintainable backend functionality using Java and Spring Boot.

## Rules

- Java 21+.
- Constructor injection.
- No field injection.
- Prefer immutable objects.
- Use records for suitable DTOs.
- Use Bean Validation.
- Use java.time.
- Keep controllers thin.
- Keep business logic outside controllers.
- Define transaction boundaries explicitly.
- Use centralized exception handling.

## Flow

HTTP Request
    ↓
Request DTO
    ↓
Controller
    ↓
Application Service
    ↓
Domain
    ↓
Repository
    ↓
Database

## Persistence

Do not expose JPA entities through REST APIs.

Use separate:

- request DTOs
- response DTOs
- persistence entities

## Exception Handling

Use @RestControllerAdvice.

Map application/domain errors to stable API error codes.

Never expose stack traces.

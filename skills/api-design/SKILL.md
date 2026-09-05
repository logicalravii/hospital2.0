# API Design Skill

## Purpose

Design consistent REST APIs for the Hospital Management System.

## Principles

- Resource-oriented URLs.
- /api/v1 versioning.
- Correct HTTP methods.
- Correct status codes.
- Request validation.
- Consistent errors.
- Pagination.
- DTO-based boundaries.
- Explicit authorization.

## Example

POST /api/v1/patients

GET /api/v1/patients/{id}

GET /api/v1/patients?page=0&size=20

PUT /api/v1/patients/{id}

## Process

Before implementing an endpoint:

1. Identify the use case.
2. Identify the resource.
3. Define request DTO.
4. Define response DTO.
5. Define validation.
6. Define authorization.
7. Define success response.
8. Define failure cases.
9. Consider idempotency.
10. Add tests.

Do not design APIs simply by exposing database CRUD operations.

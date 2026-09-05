# Hospital Management System — Agent Rules

## 1. Project Scope

This project is currently BACKEND ONLY.

Primary technologies:

- Java 21+
- Spring Boot
- Spring Data JPA
- Hibernate
- PostgreSQL
- Flyway
- Maven

Do NOT create or modify:

- Angular
- React
- Vue
- HTML/CSS
- Frontend applications
- UI components

Focus exclusively on backend architecture, domain modeling,
REST APIs, persistence, security, testing, and system design.

---

## 2. Architecture

Start with a MODULAR MONOLITH.

Initial business modules:

- identity
- patient
- doctor
- appointment
- consultation
- admission
- pharmacy
- laboratory
- billing
- notification

Do NOT introduce microservices unless explicitly requested.

Modules must have clear boundaries so that selected modules
can later be extracted into independent services.

---

## 3. Development Priorities

Prioritize:

1. Correctness
2. Domain/business correctness
3. Security
4. Maintainability
5. Testability
6. Simplicity
7. Performance
8. Scalability

Avoid premature optimization and unnecessary complexity.

Do not introduce a framework, library, pattern, or infrastructure
component without a clear reason.

---

## 4. Agent Workflow

For significant changes:

1. Understand the requirement.
2. Inspect the existing architecture.
3. Identify affected modules.
4. Check existing conventions.
5. Identify reusable components.
6. Propose the implementation approach.
7. Implement the smallest clean solution.
8. Add/update tests.
9. Run build and tests.
10. Review changed files.
11. Report important architectural decisions.

Do not silently change architectural boundaries.

Do not invent business requirements.

If an ambiguity materially affects the design,
highlight it before implementation.

---

## 5. Domain-Driven Design

Model important hospital concepts explicitly.

Avoid generic abstractions such as:

- Manager
- Helper
- Util
- CommonService
- GenericProcessor

Business rules should live close to the domain behavior
they govern.

Do not put business logic in:

- Controllers
- Repositories
- DTOs
- Configuration classes
- Database migrations

Avoid anemic domain models when meaningful domain behavior exists.

---

## 6. SOLID

Follow SOLID principles pragmatically.

In particular:

- Single Responsibility
- Open/Closed
- Liskov Substitution
- Interface Segregation
- Dependency Inversion

Do not create interfaces merely because a class has an implementation.

Use composition over inheritance where appropriate.

Use design patterns only when they solve a real problem.

---

## 7. Package Structure

Prefer feature-based packaging.

Example:

com.example.hms

    identity/
        api/
        application/
        domain/
        infrastructure/

    patient/
        api/
        application/
        domain/
        infrastructure/

    doctor/
    appointment/
    consultation/
    admission/
    pharmacy/
    laboratory/
    billing/
    notification/

Avoid a global structure such as:

    controller/
    service/
    repository/
    entity/

for the entire application.

---

## 8. REST APIs

Use resource-oriented REST APIs.

Example:

GET    /api/v1/patients/{id}
POST   /api/v1/patients
PUT    /api/v1/patients/{id}
DELETE /api/v1/patients/{id}

Rules:

- Use /api/v1.
- Use appropriate HTTP methods.
- Use appropriate HTTP status codes.
- Validate requests.
- Use DTOs at API boundaries.
- Never expose JPA entities directly.
- Use pagination for large collections.
- Keep controllers thin.
- Use consistent error responses.
- Do not design APIs solely around database tables.

---

## 9. Java and Spring Boot

Use modern Java.

Rules:

- Java 21+.
- Constructor injection only.
- Do not use field injection.
- Prefer immutable objects where appropriate.
- Use records for suitable DTOs.
- Use Bean Validation.
- Use java.time APIs.
- Define transaction boundaries explicitly.
- Keep controllers thin.
- Keep repositories focused on persistence.
- Use meaningful exceptions.
- Use centralized exception handling.

---

## 10. PostgreSQL

PostgreSQL is the primary database.

Use:

- Spring Data JPA
- Hibernate
- Flyway

Rules:

- Every table has a primary key.
- Use UUIDs where appropriate for identifiers.
- Use foreign keys for relational integrity.
- Add indexes based on actual access patterns.
- Avoid unnecessary indexes.
- Normalize core business data.
- Use JSONB for genuinely flexible/document-like data.
- Do not use JSONB as a replacement for relational modeling.
- Use appropriate constraints.
- Store timestamps consistently in UTC.
- Avoid database-specific features unless there is a clear reason.

---

## 11. Database Migrations

Flyway owns schema evolution.

Example:

V1__create_patient_table.sql
V2__create_doctor_table.sql
V3__create_appointment_table.sql

Rules:

- Every schema change requires a migration.
- Never manually modify a Flyway-managed schema.
- Never edit an already-applied migration.
- Keep migrations deterministic and reviewable.
- Add constraints and indexes through migrations.

---

## 12. Persistence

Use JPA/Hibernate carefully.

Pay particular attention to:

- lazy loading
- N+1 queries
- transaction boundaries
- cascade behavior
- entity relationships
- optimistic locking
- pagination

Do not use:

CascadeType.ALL

automatically.

Do not make every relationship bidirectional unless necessary.

Do not expose persistence entities through REST APIs.

---

## 13. Security

Security is a first-class requirement.

The system should support:

- authentication
- authorization
- RBAC
- audit logging

Potential roles:

- ADMIN
- DOCTOR
- NURSE
- RECEPTIONIST
- PHARMACIST
- LAB_TECHNICIAN
- BILLING_STAFF

These are initial candidates, not final business requirements.

Never log:

- passwords
- access tokens
- refresh tokens
- secrets
- unnecessary patient information

Follow least privilege.

---

## 14. Healthcare Data

Patient and clinical information must be treated as sensitive.

Rules:

- Minimize data exposure.
- Return only data required by the use case.
- Avoid sensitive data in logs.
- Audit important clinical/security operations.
- Do not expose internal identifiers unnecessarily.
- Do not invent medical business rules.

The system should distinguish between administrative,
clinical, financial, and security-related operations.

---

## 15. Transactions

Use transactions around meaningful business operations.

Do not annotate every method with @Transactional.

A transaction should represent a coherent unit of business work.

Be particularly careful when an operation modifies
multiple entities or aggregates.

---

## 16. Error Handling

Use centralized exception handling.

Example error:

{
    "code": "PATIENT_NOT_FOUND",
    "message": "Patient was not found",
    "timestamp": "2026-09-01T10:00:00Z",
    "path": "/api/v1/patients/123"
}

Do not expose:

- stack traces
- SQL errors
- internal class names
- implementation details

Use stable application error codes.

---

## 17. Testing

Every meaningful business feature must have tests.

Use:

- JUnit 5
- Mockito
- Spring Boot Test
- Testcontainers

Test:

- business rules
- validation
- state transitions
- persistence
- REST behavior
- error cases
- transaction behavior where relevant

Do not write tests merely to increase coverage.

Tests should verify behavior.

---

## 18. Observability

Use useful structured logging.

Do not log sensitive patient information.

When observability is introduced, prefer:

- Micrometer
- OpenTelemetry
- metrics
- tracing

Do not introduce observability infrastructure prematurely.

---

## 19. External Integrations

Keep external integrations behind adapters/interfaces.

Examples:

- Payment Gateway
- SMS
- Email
- Insurance
- External Laboratory
- Pharmacy Supplier

Third-party SDK types should not leak into the domain layer.

---

## 20. Performance

Do not optimize without evidence.

When performance becomes relevant:

1. Measure.
2. Inspect SQL.
3. Check indexes.
4. Check N+1 queries.
5. Check pagination.
6. Optimize.
7. Measure again.

Do not introduce Redis or Kafka merely because they are commonly used.

---

## 21. Git

Keep commits focused.

Use:

type(scope): description

Examples:

feat(patient): add patient registration

feat(appointment): implement appointment booking

fix(billing): prevent duplicate invoice

test(patient): add registration tests

Do not mix unrelated changes.

---

## 22. Definition of Done

Before declaring a task complete:

- Application builds successfully.
- Relevant tests pass.
- New business behavior has tests.
- Database changes have Flyway migrations.
- API validation is implemented.
- Error handling is covered.
- Security implications have been considered.
- No sensitive information is committed.
- No unrelated files were changed.
- Architecture remains consistent.

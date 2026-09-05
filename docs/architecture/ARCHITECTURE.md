# Hospital Management System Architecture

## Current Architecture

The system is a backend-only **Microservices Architecture** organized as a Maven multi-module monorepo.

### Core Technologies:
- **Language**: Java 25
- **Framework**: Spring Boot 4.x
- **API Gateway**: Spring Cloud Gateway (Port 8080)
- **Event Streaming**: Apache Kafka (KRaft mode)
- **Database**: PostgreSQL 16 (Database-per-Service)
- **Persistence**: Spring Data JPA / Hibernate
- **Schema Migrations**: Flyway (Independent migration streams per service)
- **Containerization**: Docker & Docker Compose
- **Testing**: JUnit 5, Mockito, Testcontainers (PostgreSQL & Kafka)

---

## Core Microservices (PoC Scope)

1. **`api-gateway`** (Port 8080): Unified edge routing, security context header forwarding (`X-User-Id`, `X-User-Roles`).
2. **`identity-service`** (Port 8081, DB: `identity_db`): User management, RBAC, JWT authentication.
3. **`patient-service`** (Port 8082, DB: `patient_db`): Patient demographics, unique MRN generation, emits `PatientRegisteredEvent`.
4. **`doctor-service`** (Port 8083, DB: `doctor_db`): Doctor credentials, specialization, working schedules.
5. **`appointment-service`** (Port 8084, DB: `appointment_db`): Slot booking, appointment lifecycle state machine, emits `AppointmentScheduledEvent`.
6. **`consultation-service`** (Port 8085, DB: `consultation_db`): Clinical encounters, diagnoses, vitals stored as structured JSONB, emits `ConsultationCompletedEvent`.

---

## Inter-Service Communication Patterns

- **Synchronous (REST/HTTP)**: Used for direct queries requiring immediate consistency (e.g. `appointment-service` querying `doctor-service` schedule availability).
- **Asynchronous (Apache Kafka)**: Used for state notifications, decoupled workflows, and audit logging (`hospital.patient.events`, `hospital.appointment.events`, `hospital.consultation.events`).

---

## Data Management & Integrity

- **Database-per-Service**: Each service owns its schema. Direct cross-database queries or physical foreign keys are strictly prohibited.
- **Logical References**: Inter-service relationships use logical UUIDs (`patient_id`, `doctor_id`, `appointment_id`).
- **Eventual Consistency**: Workflows crossing microservice boundaries are coordinated asynchronously via Kafka events.

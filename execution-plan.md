# Hospital Management System — Microservices Execution Tracker

This document tracks the step-by-step implementation of the Hospital Management System (HMS) microservices architecture. Each step is implemented independently so it can be cleanly reviewed and verified before proceeding to the next.

---

## Progress Overview

| Step | Component / Phase | Status | Review & Verification |
| :--- | :--- | :---: | :--- |
| **0** | Architecture & PoC Documentation | `COMPLETED` | [POC.md](file:///Users/ravikumar/Test_Application/hospital/docs/POC.md) & [ARCHITECTURE.md](file:///Users/ravikumar/Test_Application/hospital/docs/architecture/ARCHITECTURE.md) updated |
| **1** | Root Parent Maven POM & Monorepo Setup | `COMPLETED` | Parent POM aggregator configured, build clean |
| **2** | Shared Common Module (`common`) | `COMPLETED` | Events, Error model, Security utilities, 10 tests passed |
| **3** | Docker Infrastructure (PostgreSQL Multi-DB & Kafka) | `COMPLETED` | PostgreSQL (5 isolated DBs) & Kafka KRaft healthy |
| **4** | Identity Service (`identity-service` :8081) | `COMPLETED` | User accounts, JWT auth, Flyway `V1`, 14 tests pass, live verified |
| **5** | Patient Service (`patient-service` :8082) | `NOT STARTED` | Registration, MRN, Kafka Producer |
| **6** | Doctor Service (`doctor-service` :8083) | `NOT STARTED` | Profiles, Schedules, Availability queries |
| **7** | Appointment Service (`appointment-service` :8084) | `NOT STARTED` | Booking FSM, Kafka Producer & Consumer |
| **8** | Consultation Service (`consultation-service` :8085) | `NOT STARTED` | Clinical encounters, Vitals JSONB, Kafka |
| **9** | API Gateway (`api-gateway` :8080) | `NOT STARTED` | Spring Cloud Gateway routing & header forward |
| **10** | End-to-End Verification & Verification Suite | `NOT STARTED` | Full flow smoke testing across gateway |

---

## Detailed Step Breakdown

### Step 0: Architecture & PoC Documentation
- [x] Update [POC.md](file:///Users/ravikumar/Test_Application/hospital/docs/POC.md) to document the Microservices Architecture.
- [x] Define Apache Kafka (KRaft mode) event streaming specifications and topics.
- [x] Define Database-per-Service schema models and logical foreign keys.
- [x] Update [ARCHITECTURE.md](file:///Users/ravikumar/Test_Application/hospital/docs/architecture/ARCHITECTURE.md).

---

### Step 1: Root Parent Maven POM & Monorepo Setup
- [x] Restructure root [pom.xml](file:///Users/ravikumar/Test_Application/hospital/pom.xml) into a parent aggregator (`<packaging>pom</packaging>`).
- [x] Configure `<dependencyManagement>` for Spring Boot 4.x, Spring Cloud, Spring Kafka, PostgreSQL, Flyway, and Testcontainers.
- [x] Remove monolithic placeholder source (`src/main/java/com/management/hospital/HospitalApplication.java`).
- [x] **Verification**: Run `./mvnw clean` to ensure parent POM builds cleanly.

---

### Step 2: Shared Common Module (`common`)
- [x] Create `common/pom.xml` with shared base dependencies.
- [x] Create centralized API error model (`ApiErrorResponse`) and domain exception classes (`DomainException`, `ResourceNotFoundException`, `ConflictException`, `UnauthorizedException`, `ForbiddenException`).
- [x] Create immutable Java Records for Kafka domain events:
  - `PatientRegisteredEvent`
  - `AppointmentScheduledEvent`
  - `AppointmentCancelledEvent`
  - `ConsultationCompletedEvent`
- [x] Create security context helpers (`AuthenticatedUser`, `UserContextExtractor`, `SecurityHeaders`).
- [x] **Verification**: Build and install `common` via `./mvnw clean install -pl common` (10 unit tests passing).

---

### Step 3: Docker Compose Infrastructure (PostgreSQL & Kafka)
- [x] Create PostgreSQL initialization script (`init-multiple-databases.sh`) to automatically provision:
  - `identity_db`, `patient_db`, `doctor_db`, `appointment_db`, `consultation_db`.
- [x] Create [docker-compose.yml](file:///Users/ravikumar/Test_Application/hospital/docker-compose.yml) orchestrating:
  - PostgreSQL 16 container with persistent volumes.
  - Apache Kafka (`confluentinc/cp-kafka:7.7.0`) in KRaft mode (single-node, no ZooKeeper).
- [x] **Verification**: Run `docker compose up -d postgres kafka` and verify both report healthy.

---

### Step 4: Identity Service (`services/identity-service` — Port 8081)
- [x] Create `services/identity-service/pom.xml` and application bootstrap class.
- [x] Add Flyway migration `V1__init_identity.sql` (`app_user`, `user_role` tables).
- [x] Implement Domain layer: `UserAccount` aggregate, `Role` enum, password hashing invariants.
- [x] Implement Application layer: `AuthenticationService` issuing signed JWT tokens.
- [x] Implement REST API: `POST /api/v1/auth/login` and `POST /api/v1/auth/register`.
- [x] Add unit and slice tests for authentication (14 tests passing).
- [x] **Verification**: Run tests and verify JWT issuance via HTTP call against live PostgreSQL `identity_db`.

---

### Step 5: Patient Service (`services/patient-service` — Port 8082)
- [ ] Create `services/patient-service/pom.xml` and application bootstrap class.
- [ ] Add Flyway migration `V1__init_patient.sql` (`patient` table with unique MRN).
- [ ] Implement Domain layer: `Patient` aggregate, `EmergencyContact` value object, MRN generation.
- [ ] Configure Kafka Producer for topic `hospital.patient.events`.
- [ ] Implement REST API: `POST /api/v1/patients` (emits `PatientRegisteredEvent`) and `GET /api/v1/patients/{id}`.
- [ ] Add unit and repository tests.
- [ ] **Verification**: Register a patient and verify Kafka event publication.

---

### Step 6: Doctor Service (`services/doctor-service` — Port 8083)
- [ ] Create `services/doctor-service/pom.xml` and application bootstrap class.
- [ ] Add Flyway migration `V1__init_doctor.sql` (`doctor`, `doctor_schedule` tables).
- [ ] Implement Domain layer: `Doctor` aggregate, `DoctorSchedule` entity, non-overlapping slot invariants.
- [ ] Implement REST API: `POST /api/v1/doctors`, `POST /api/v1/doctors/{id}/schedules`, and `GET /api/v1/doctors/{id}/schedules`.
- [ ] Add unit and domain tests.
- [ ] **Verification**: Register a doctor, create schedule blocks, and query available slots.

---

### Step 7: Appointment Service (`services/appointment-service` — Port 8084)
- [ ] Create `services/appointment-service/pom.xml` and application bootstrap class.
- [ ] Add Flyway migration `V1__init_appointment.sql` (`appointment` table).
- [ ] Implement Domain layer: `Appointment` aggregate with lifecycle state machine (`SCHEDULED` -> `CONFIRMED` -> `IN_PROGRESS` -> `COMPLETED` / `CANCELLED`).
- [ ] Implement synchronous HTTP client (with Resilience4j) to verify doctor schedule against `doctor-service`.
- [ ] Configure Kafka Producer for `hospital.appointment.events` (`AppointmentScheduledEvent`, `AppointmentCancelledEvent`).
- [ ] Configure Kafka Consumer for `hospital.consultation.events` to auto-complete appointments upon consultation finish.
- [ ] Implement REST API: `POST /api/v1/appointments`, `POST /api/v1/appointments/{id}/cancel`, `GET /api/v1/appointments/{id}`.
- [ ] Add tests for appointment state machine and scheduling invariants.
- [ ] **Verification**: Book an appointment, verify Kafka event emitted, and test cancellation flow.

---

### Step 8: Consultation Service (`services/consultation-service` — Port 8085)
- [ ] Create `services/consultation-service/pom.xml` and application bootstrap class.
- [ ] Add Flyway migration `V1__init_consultation.sql` (`consultation` table with `vitals JSONB`).
- [ ] Implement Domain layer: `Consultation` aggregate, `Vitals` value object with physiological boundary validation.
- [ ] Configure Kafka Producer for `hospital.consultation.events` (`ConsultationCompletedEvent`).
- [ ] Implement REST API: `POST /api/v1/consultations` and `GET /api/v1/consultations/{id}`.
- [ ] Add tests for vitals JSONB serialization and clinical domain invariants.
- [ ] **Verification**: Record a consultation, verify vitals JSONB storage and Kafka event publication.

---

### Step 9: API Gateway (`services/api-gateway` — Port 8080)
- [ ] Create `services/api-gateway/pom.xml` with Spring Cloud Gateway dependency.
- [ ] Configure routes routing `/api/v1/**` to the respective microservices (`8081`–`8085`).
- [ ] Implement JWT verification filter and downstream header propagation (`X-User-Id`, `X-User-Roles`).
- [ ] **Verification**: Send requests to port `8080` and verify routing to underlying services.

---

### Step 10: End-to-End System Verification
- [ ] Add multi-stage Dockerfiles for all services.
- [ ] Update [docker-compose.yml](file:///Users/ravikumar/Test_Application/hospital/docker-compose.yml) with all service containers and dependencies.
- [ ] Execute automated full patient journey smoke test through the API Gateway:
  - Staff Login -> Register Patient -> Register Doctor -> Configure Schedule -> Book Appointment -> Record Consultation -> Verify Appointment marked Completed.

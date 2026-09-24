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
| **5** | Patient Service (`patient-service` :8082) | `COMPLETED` | Registration, MRN, Flyway `V1`, 13 tests pass, Kafka event verified |
| **6** | Doctor Service (`doctor-service` :8083) | `COMPLETED` | Profiles, Schedules, Flyway `V1`, 19 tests pass, slot queries verified |
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
- [x] Add unit and slice tests for authentication (16 tests passing).
- [x] **Verification**: Run tests and verify JWT issuance via HTTP call against live PostgreSQL `identity_db`.

---

### Step 5: Patient Service (`services/patient-service` — Port 8082)
- [x] Create `services/patient-service/pom.xml` and application bootstrap class.
- [x] Add Flyway migration `V1__init_patient.sql` (`patient` table with unique MRN, phone, and indexing).
- [x] Implement Domain layer: `Patient` aggregate, `Address` and `EmergencyContact` value objects, `MedicalRecordNumberGenerator`.
- [x] Configure Kafka Producer with `spring-boot-starter-kafka` for topic `hospital.patient.events`.
- [x] Implement REST API:
  - `POST /api/v1/patients` (registers patient, generates MRN, emits `PatientRegisteredEvent` to Kafka).
  - `GET /api/v1/patients/{id}` (fetch patient by UUID).
  - `GET /api/v1/patients/mrn/{mrn}` (fetch patient by MRN).
  - `GET /api/v1/patients` (paginated patient list).
- [x] Add 13 automated tests across domain, service, and web layers.
- [x] **Verification**:
  - Live PostgreSQL database migration verified (`patient` table in `patient_db`).
  - Live REST API verified via HTTP calls (201 Created, 200 OK, 400 Bad Request, 404 Not Found, 409 Conflict).
  - Kafka domain event publication verified on `hospital.patient.events` topic using `kafka-console-consumer`.
  - HTTP test requests saved in [docs/api-test/patient/api-test.HTTP](file:///Users/ravikumar/Test_Application/hospital/docs/api-test/patient/api-test.HTTP).

---

### Step 6: Doctor Service (`services/doctor-service` — Port 8083)
- [x] Create `services/doctor-service/pom.xml` and application bootstrap class.
- [x] Add Flyway migration `V1__init_doctor.sql` (`doctor` and `doctor_schedule` tables).
- [x] Implement Domain layer: `Doctor` aggregate, `DoctorSchedule` entity, `Specialty` enum, `TimeSlot` record, non-overlapping slot invariants.
- [x] Implement REST API:
  - `POST /api/v1/doctors` (register doctor profile, returns 201).
  - `GET /api/v1/doctors/{id}` (retrieve doctor by ID, returns 200).
  - `GET /api/v1/doctors` (list doctors with specialty filtering and pagination, returns 200).
  - `POST /api/v1/doctors/{id}/schedules` (configure weekly hours, validates overlap, returns 201).
  - `GET /api/v1/doctors/{id}/schedules` (list all doctor schedule blocks, returns 200).
  - `GET /api/v1/doctors/{id}/slots?date=YYYY-MM-DD` (query available appointment slots for a specific date, returns 200).
- [x] Add 19 automated tests across domain, service, and web layers.
- [x] **Verification**:
  - Flyway migration verified in live PostgreSQL `doctor_db`.
  - Live REST API verified via HTTP calls (201 Created, 200 OK, 400 Bad Request, 404 Not Found, 409 Conflict).
  - Date-based available slot generation verified.
  - HTTP test requests saved in [docs/api-test/doctor/api-test.HTTP](file:///Users/ravikumar/Test_Application/hospital/docs/api-test/doctor/api-test.HTTP).

---

### Step 7: Appointment Service (`services/appointment-service` — Port 8084)
- [x] Create `services/appointment-service/pom.xml` and application bootstrap class.
- [x] Add Flyway migration `V1__init_appointment.sql` (`appointment` table).
- [x] Implement Domain layer: `Appointment` aggregate with lifecycle state machine (`SCHEDULED` -> `CONFIRMED` -> `IN_PROGRESS` -> `COMPLETED` / `CANCELLED`).
- [x] Implement synchronous HTTP client (`DoctorServiceClient` using Spring `RestClient`) to verify doctor schedule against `doctor-service`.
- [x] Configure Kafka Producer for `hospital.appointment.events` (`AppointmentScheduledEvent`, `AppointmentCancelledEvent`).
- [x] Configure Kafka Consumer for `hospital.consultation.events` to auto-complete appointments upon consultation finish.
- [x] Implement REST API:
  - `POST /api/v1/appointments` (book appointment, returns 201).
  - `POST /api/v1/appointments/{id}/confirm` (confirm appointment, returns 200).
  - `POST /api/v1/appointments/{id}/start` (start appointment, returns 200).
  - `POST /api/v1/appointments/{id}/cancel` (cancel appointment with reason, returns 200).
  - `GET /api/v1/appointments/{id}` (get appointment by ID, returns 200).
  - `GET /api/v1/appointments` (query/filter appointments by doctor, patient, date, status with pagination, returns 200).
- [x] Add 15 automated tests covering state machine transitions, scheduling conflict invariants, and REST controller endpoints.
- [x] **Verification**:
  - Flyway migration applied to live PostgreSQL `appointment_db`.
  - Live REST endpoints verified (booking, double-booking rejection with HTTP 409 `CONFLICTING_STATE`, confirmation, start, cancellation, querying).
  - Kafka domain event publications verified on `hospital.appointment.events` (`AppointmentScheduledEvent`, `AppointmentCancelledEvent`) via `kafka-console-consumer`.
  - HTTP test requests saved in [docs/api-test/appointment/api-test.HTTP](file:///Users/ravikumar/Test_Application/hospital/docs/api-test/appointment/api-test.HTTP).

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

# PostgreSQL Skill

## Purpose

Design reliable relational persistence using PostgreSQL,
JPA/Hibernate, and Flyway.

## Core Modeling

Use relational tables for core business entities.

Examples:

patient
doctor
appointment
consultation
prescription
medicine
lab_order
lab_result
admission
bed
invoice
payment

## JSONB

Use JSONB when data is genuinely flexible.

Good example:

consultation.vitals JSONB

Possible structure:

{
    "temperature": 98.6,
    "bloodPressure": {
        "systolic": 120,
        "diastolic": 80
    },
    "heartRate": 72
}

Do NOT use JSONB for core relationships such as:

Patient → Appointment
Appointment → Doctor
Prescription → Medicine

These should remain relational.

## Rules

- Use primary keys.
- Use foreign keys.
- Use appropriate indexes.
- Use unique constraints where required.
- Normalize core business data.
- Use UTC timestamps.
- Avoid unnecessary database-specific coupling.

## JPA

Be careful with:

- lazy/eager loading
- N+1 queries
- cascading
- orphan removal
- bidirectional relationships
- optimistic locking
- pagination

Do not blindly use CascadeType.ALL.

## Flyway

All schema changes require migrations.

Example:

V1__create_patient_table.sql
V2__create_doctor_table.sql
V3__create_appointment_table.sql

Never edit an already-applied migration.

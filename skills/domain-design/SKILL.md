# Domain Design Skill

## Purpose

Design hospital business concepts before implementation.

Use when introducing:

- a new module
- a business entity
- a workflow
- a business rule
- a state transition

## Process

1. Identify actors.
2. Identify use cases.
3. Identify domain entities.
4. Identify value objects.
5. Identify relationships.
6. Identify invariants.
7. Identify state transitions.
8. Identify domain events where useful.
9. Define module ownership.
10. Design persistence.
11. Design APIs.

Do not start with database tables.

## Rules

- Use meaningful domain terminology.
- Keep business rules explicit.
- Avoid generic abstractions.
- Avoid anemic models where domain behavior exists.
- Avoid premature microservice boundaries.
- Keep domain concepts independent of infrastructure where practical.

## Example

Appointment may contain concepts such as:

- Patient
- Doctor
- AppointmentSlot
- AppointmentType
- AppointmentStatus

Possible states:

SCHEDULED
CONFIRMED
CANCELLED
COMPLETED
NO_SHOW

Do not treat these states as final until requirements establish them.

# System Design Skill

## Purpose

Analyze how the HMS can evolve from a modular monolith
into a scalable distributed system when justified.

## Initial Architecture

Use a modular monolith.

Do not introduce microservices prematurely.

## Analyze

For major architectural decisions consider:

- traffic
- data ownership
- consistency
- availability
- latency
- failure modes
- operational complexity
- deployment independence
- team ownership

## Microservice Extraction

Before extracting a module:

1. Identify its bounded context.
2. Define data ownership.
3. Identify dependencies.
4. Identify synchronous communication.
5. Identify asynchronous events.
6. Define service APIs.
7. Define transaction boundaries.
8. Plan data migration.
9. Define failure/retry behavior.
10. Define observability.
11. Evaluate operational cost.

Only then extract the service.

## Infrastructure

Possible future technologies:

- Kafka
- Redis
- API Gateway
- Kubernetes
- OpenTelemetry
- Prometheus
- Grafana

Do not introduce them unless a concrete requirement justifies them.

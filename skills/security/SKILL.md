# Security Skill

## Purpose

Design authentication, authorization, and audit behavior securely.

## Principles

- Least privilege.
- Defense in depth.
- Secure password storage.
- Explicit authorization.
- Minimal data exposure.
- Secure secret management.
- Audit important operations.

## RBAC

Initially support role-based access control.

Authorization must be enforced on the backend.

Never rely on frontend authorization.

## Sensitive Data

Never log:

- passwords
- tokens
- secrets
- unnecessary patient data

## Security Testing

Security-sensitive features must test:

- authenticated access
- unauthenticated access
- authorized access
- unauthorized access
- role restrictions

## Important

Do not invent security requirements.

When a feature exposes patient, clinical, or financial data,
explicitly consider authorization and audit requirements.

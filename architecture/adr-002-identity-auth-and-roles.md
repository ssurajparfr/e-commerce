# ADR-002 — Identity, authentication, and roles

- **Status:** Accepted; implementation starts in job 002
- **Date:** 2026-10-25

## Context

The first increment is guest checkout and has no customer accounts. Later
increments add registration, login, account management, order history, and a
protected admin/operations console. The browser is the primary client, so
server-side sessions are a better fit than tokens in browser storage.

## Decision

- Use Spring Security with role-based access control.
- Roles: `CUSTOMER`, `ADMIN`. Operations endpoints require `ADMIN` (or a
  dedicated `OPERATOR` role if a later job justifies it; do not add roles
  speculatively).
- Use server-side sessions persisted in PostgreSQL via Spring Session JDBC.
  Do not use JWTs in browser storage for the MVP.
- Session cookie: `HttpOnly`, `SameSite=Lax`, `Secure` outside local
  development. Session timeout and rotation are explicit configuration.
- Hash passwords with BCrypt at strength 12 (or Argon2id if the team adopts it
  uniformly). Never store or log plaintext passwords.
- Enable CSRF protection for browser state-changing requests. The frontend must
  send the CSRF token; API clients use the same protection path.
- Restrict CORS to the configured local frontend origin. Do not use `*` with
  credentials.
- Rate-limit login, registration, and password-reset endpoints where practical.
- All admin mutations require authorization and produce an audit record.

## Job 001 boundary

Guest checkout uses an anonymous cart cookie, not an authenticated session.
Job 001 must not add login, registration, or account endpoints. The anonymous
cookie contains only an opaque cart identifier and must not contain personal
data.

## Consequences

- Sessions survive application restarts because they live in PostgreSQL.
- CSRF and cookie rules are part of every state-changing endpoint review.
- The future investigator must use the same authorization paths; it never gets
  a database credential or a bypass.

## Alternatives considered

- JWT in local storage: rejected for the browser MVP because of token theft and
  revocation complexity.
- In-memory sessions: rejected because they do not survive restarts and are not
  a durable base for the operations console.

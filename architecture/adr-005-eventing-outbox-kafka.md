# ADR-005 — Eventing, transactional outbox, and Kafka

- **Status:** Accepted; implementation starts in job 003
- **Date:** 2026-10-25

## Context

Later increments publish domain events asynchronously. Publishing directly to
Kafka from a request risks losing an event when the database commit succeeds but
the broker call fails, or publishing an event for a transaction that later
rolls back. Consumers must tolerate retries and duplicates.

## Decision

- Use a transactional outbox: business state and the outgoing event row commit
  in the same PostgreSQL transaction. A publisher relays committed outbox rows
  to Kafka and marks them published.
- Event envelope fields: event id, event type, schema version, aggregate id,
  correlation id, causation id, occurrence timestamp, producer identifier, trace
  context, and payload. Never include passwords, payment secrets, access tokens,
  or unnecessary personal data.
- Version event contracts. A breaking change requires a new major version and a
  documented compatibility window. Additive fields are backward compatible.
- Consumers are idempotent: record processed event ids per consumer and treat
  redelivery as a no-op for business side effects.
- Use bounded consumer retries with backoff. After the limit, route the message
  to a dead-letter topic with error details; preserve the processing history.
- Topic naming: `ecom.<env>.<domain>.<event>.v<major>` for a durable
  environment. On the shared kind cluster, include the job id after the
  environment (`ecom.local.<job-id>.<domain>.<event>.v<major>`) to avoid
  collisions. Dead-letter topics use a `.dlq` suffix.
- Replay is an explicit, documented procedure that reuses idempotency records so
  it cannot duplicate business side effects.
- Do not claim exactly-once business outcomes. Document delivery semantics and
  acknowledge that Kafka guarantees alone do not provide cross-system
  exactly-once behavior.
- Provide scripts or documented commands to inspect topics, consumer groups,
  lag, and failed messages.

## Consequences

- Event publication is reliable after the database commit, including after a
  restart.
- Operational tables for outbox, processed events, and dead letters are part of
  the diagnostics surface.
- Schema evolution and replay require discipline, not ad-hoc topic edits.

## Alternatives considered

- Direct publish from the request thread: rejected because it loses atomicity
  with the database commit.
- Kafka transactions for business exactly-once: rejected as insufficient across
  PostgreSQL and external providers.

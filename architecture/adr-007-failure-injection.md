# ADR-007 — Controlled failure injection

- **Status:** Accepted; implementation starts in the incident-lab job
- **Date:** 2026-10-25

## Context

The future investigator needs deterministic incidents to investigate. Random
failures in normal operation would make the platform unreliable and the evidence
irreproducible. Failure injection must never be an accidental production
feature.

## Decision

- Failure injection is development/test only and disabled by default.
- It requires all of: an explicit development/test Spring profile, an
  authenticated administrator, a bounded duration, and an audit record.
- The application refuses to start with failure injection enabled under a
  production profile.
- Prefer named, deterministic scenarios over uncontrolled randomness. Each
  scenario defines preconditions, injection mechanism, expected customer-visible
  symptoms, expected logs/metrics/traces, expected business-state transitions,
  recovery behavior, and a verification procedure.
- Scenarios are allowlisted and scoped: payment timeout, payment intermittent
  failure, payment decline, inventory reservation conflict, Kafka consumer
  paused/delayed, notification outage, duplicate event delivery, message
  processing exception, Redis unavailable, increased dependency latency, outbox
  publication delay.
- Injection must never corrupt unrelated data or bypass core business
  invariants.
- Provide a documented way to disable an active scenario and return to normal.
- Automated integration tests and runbooks cover every scenario.

## Consequences

- The incident lab can replay the same evidence for a human and for the future
  AI investigator.
- Scenario state is operational data with an audit trail, not a hidden global
  variable.

## Alternatives considered

- Random fault injection in normal operation: rejected because it destroys
  determinism and reproducibility.
- Production feature flags for injection: rejected; the risk is not worth it.

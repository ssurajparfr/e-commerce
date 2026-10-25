# ADR-006 — Observability, diagnostics, and the future investigator

- **Status:** Accepted; full stack starts in the observability job
- **Date:** 2026-10-25

## Context

The platform must be investigable from day one. A future LangGraph4j + Spring AI
application will gather evidence through approved read-only tools, build an order
timeline, and propose remediation. It must not need database credentials,
unrestricted Kafka access, shell access, or arbitrary SQL.

## Decision

### Correlation and tracing

- Generate a correlation id at the edge (HTTP request or scheduled job) and
  propagate it through synchronous calls, persisted workflow records, and Kafka
  headers/payload metadata.
- Use W3C trace context (`traceparent`) across HTTP and Kafka boundaries.
- Record causation id for events triggered by another event.

### Structured logging

Emit JSON logs with consistent fields where applicable: timestamp, severity,
service/module, environment, trace id, span id, correlation id, causation id,
order id, event id, operation, outcome, error code, and duration in
milliseconds. Use appropriate levels. Never log secrets, credentials, full
payment data, or sensitive customer information.

### Metrics

Instrument HTTP rate/latency/errors, checkout success/failure, order-processing
duration, payment outcomes, inventory reservation failures, Kafka lag, retry and
dead-letter counts, notification failures, connection-pool utilization, Redis
errors/latency, and dependency timeouts.

Use bounded-cardinality labels only. Never use customer ids, order ids, or other
unbounded identifiers as Prometheus labels.

### Health checks

Distinguish liveness, readiness, and dependency health. Do not expose secrets or
sensitive infrastructure details to unauthenticated callers.

### Diagnostic APIs

Provide protected, read-only APIs for the future investigator, with pagination,
time-range filters, access control, and bounded query sizes. Capabilities
include: health, dependencies, order timeline, order events, payment attempts,
event lookup, dead letters, metrics summary, and incidents. Prefer structured
responses over requiring an investigator to parse log text.

### Investigator boundary

The future investigator gets narrow read tools and an allowlisted remediation
interface. It never bypasses authorization or validation. Remediation requires
explicit human approval and an audit record.

## Consequences

- Correlation and event ids are implemented while the checkout flow is built,
  not bolted on later.
- Operational APIs become the stable contract between the commerce platform and
  the future investigation application.

## Alternatives considered

- Logs only, no structured diagnostics: rejected because it makes evidence
  collection fragile and encourages unrestricted access.
- Exposing the database or Kafka directly to the investigator: rejected for
  security and least privilege.

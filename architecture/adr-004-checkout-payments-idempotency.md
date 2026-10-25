# ADR-004 — Checkout, payments, idempotency, and compensation

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

Checkout touches PostgreSQL, inventory, a payment provider, and (in later
increments) Kafka. These do not share one ACID transaction. Retries, duplicate
submissions, timeouts, and provider failures are normal. The payment provider is
a simulator; no real money or card data is involved.

## Decision

- Model checkout as a documented saga with explicit states and compensating
  actions. Never claim a single distributed transaction.
- Steps: validate customer/guest and cart → revalidate prices and availability →
  create a pending order → reserve inventory transactionally → request payment
  through the provider abstraction → confirm the order and publish events →
  notify asynchronously (later job).
- Compensation: on payment failure or timeout, cancel or mark the order failed
  and release the inventory reservation. Never leave an orphaned reservation
  without a documented recovery path.
- Payment provider abstraction hides the simulator. The simulator is
  deterministic in tests and supports configured success, decline, timeout, and
  intermittent-failure scenarios in development.
- Require an idempotency key for checkout submission and for payment requests.
  Reusing a key with the same request returns the same result; reusing it with a
  different payload is a conflict.
- Persist payment attempts, provider references, failure codes, and timestamps
  so attempts can be inspected.
- Use explicit timeouts and bounded retries with backoff. Do not retry
  unboundedly and do not use arbitrary sleeps as synchronization.
- Define explicit intermediate states (for example payment pending/review) for
  dependency timeouts instead of pretending success or leaving the order in an
  undocumented state.
- A duplicate event, duplicate request, or duplicate callback must not create a
  second order, payment, reservation, or confirmation.
- No real payment credentials, card data, or financial transactions anywhere.

## Consequences

- Some orders can be temporarily in an intermediate state; diagnostics must make
  that state and its processing history inspectable.
- Idempotency records and payment attempts are first-class operational data.
- The future investigator can correlate order state, payment attempts, retries,
  and compensating actions without direct database access.

## Alternatives considered

- Best-effort synchronous checkout with no compensation: rejected because it
  corrupts inventory or payment state on partial failure.
- Distributed transaction/XA across PostgreSQL and the provider: rejected as
  unrealistic and unsupported.

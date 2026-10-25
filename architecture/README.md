# Architecture decisions

Architecture Decision Records for CommerceOps. Each ADR records a decision that
shapes the implementation. Update an ADR only when the decision changes, and add
a new ADR for a new decision rather than silently rewriting history.

| ADR | Decision |
|---|---|
| [ADR-001](adr-001-stack-and-module-boundaries.md) | Stack, module boundaries, repository layout, toolchain, ports |
| [ADR-002](adr-002-identity-auth-and-roles.md) | Spring Security, server-side sessions, customer/admin roles |
| [ADR-003](adr-003-catalog-inventory-cart-orders.md) | Money in integer cents, EUR, VAT per line, order snapshots, inventory invariants, cart in PostgreSQL |
| [ADR-004](adr-004-checkout-payments-idempotency.md) | Mock payment provider, idempotency keys, reservation/release, compensation |
| [ADR-005](adr-005-eventing-outbox-kafka.md) | Transactional outbox, topic naming, idempotent consumers, DLQ |
| [ADR-006](adr-006-observability-and-diagnostics.md) | Correlation ids, structured logs, Micrometer/OTel, read-only diagnostics |
| [ADR-007](adr-007-failure-injection.md) | Dev/test-only failure injection with authorization, audit, bounded duration |

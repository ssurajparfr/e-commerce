# ADR-003 — Catalog, inventory, cart, orders, and money

- **Status:** Accepted
- **Date:** 2026-10-25

## Context

The first increment needs a correct commerce core: seeded catalog, cart,
checkout, inventory reservation, order snapshots, and a mock payment. The
platform must preserve business invariants under retries and concurrency, and
it must not depend on Redis for durable state.

## Decision

### Money

- Store money as integer cents. Currency is EUR.
- The MVP VAT rate is configurable, default 20%. VAT is calculated per line.
- Prices, discounts, VAT, and totals are always calculated on the server.
  Client-submitted prices are ignored or rejected.
- Persist an order-item snapshot: product description, unit price, VAT rate,
  quantity, and line total. Later product changes never rewrite historical
  orders.
- Rounding is explicit and applied consistently per line and at the total.

### Catalog

- Product has SKU, name, description, category, price, currency, active flag,
  and metadata.
- Public listing supports pagination, sorting, filtering, search, and category
  navigation. Only active products are purchasable.
- Seed data is synthetic and deterministic.

### Inventory

- Inventory holds available and reserved quantities plus a version for
  optimistic concurrency.
- A reservation must never create negative available stock. Reservation and
  release are transactional.
- Exactly one of two concurrent buyers can reserve the last unit; the other
  receives a well-defined out-of-stock outcome.
- Inventory changes from administration produce audit records.

### Cart

- The cart lives in PostgreSQL for the MVP, keyed by an anonymous cookie for
  guests. Redis is not used for the cart in job 001.
- Items store product id and quantity; price is always re-read at checkout.
- Carts expire after a documented period (default 30 days). An expired cart is
  not reused; the guest receives a new cart. A lost anonymous cookie means the
  guest cart is unrecoverable, which is acceptable for guest checkout.
- Cart operations are idempotent where retries are plausible.

### Orders

- Order stores customer or guest reference, items, price snapshots, subtotal,
  tax, total, currency, statuses (order/payment/fulfillment), idempotency key,
  correlation/trace context where appropriate, and timestamps.
- Use an explicit state machine:
  `CREATED → INVENTORY_RESERVED → PAYMENT_PENDING → CONFIRMED →
  FULFILLMENT_PENDING → SHIPPED → DELIVERED`.
  Valid failure/cancellation transitions include `INVENTORY_FAILED`,
  `PAYMENT_FAILED`, `CANCELLED`, and `EXPIRED`.
- Arbitrary status updates are rejected. Every transition is persisted with
  enough context to reconstruct a timeline.
- Add indexes for operational queries: order id, customer/guest reference,
  status, created time, idempotency key, and correlation id.

## Consequences

- PostgreSQL is the only source of truth for orders, payments, and inventory.
- Cart loss on cookie loss is an accepted guest-checkout limitation.
- Order history remains correct after catalog changes because of snapshots.

## Alternatives considered

- Redis as the cart source of truth: rejected for job 001 because it adds a
  dependency without a demonstrated need and complicates durability.
- Storing only product ids in orders: rejected because historical totals must
  be reconstructable.

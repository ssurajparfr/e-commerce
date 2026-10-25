# AGENTS.md — CommerceOps target repository

CommerceOps is a production-style e-commerce platform for France/EU. It is a
factory target: the hub is
`/home/ssura/software-factory/software-factory-open-code`, the alias is `ecom`,
and this repository is the application under construction.

- Product and decided scope: `PRODUCT.md`.
- Architecture decisions: `architecture/adr-001` … `adr-007`.
- Factory bootstrap plan: `docs/plans/2026-10-25-ecom-factory-bootstrap.md`.
- Hub backlog for this target: `factory/backlog/ecom.md` in the hub.

The application code is added incrementally by factory jobs. Do not treat this
baseline as a finished scaffold.

## Pinned stack

| Area | Choice |
|---|---|
| Backend | Java 21, Spring Boot 4.1.2, Spring MVC, Spring Security, Spring Data JPA, Flyway, Spring Actuator, Maven Wrapper |
| Frontend | React 19.3.1, Vite 8.3.5, TypeScript 7.0.2, React Router, TanStack Query, React Hook Form, Tailwind CSS, Vitest, React Testing Library, Playwright |
| Data | PostgreSQL 17 as source of truth; Redis only behind an optional profile when a job needs it |
| Async | Apache Kafka via the shared kind cluster only when a job needs it; Testcontainers for tests |
| Tooling | pnpm 12.15.0, Maven Wrapper 3.9.x, Node 24.x, `uv` for Python helpers only |

Verify a version is resolvable before pinning it in a job. This baseline was
checked against Maven Central and the npm registry on 2026-10-25. If a pinned
version is unavailable, choose the nearest supported stable release and update
this table plus ADR-001 in the same job.

## Planned repository layout

```text
backend/
  src/main/java/com/example/commerce/
    identity/ catalog/ inventory/ cart/ checkout/ orders/ payments/
    notifications/ eventprocessing/ administration/ operations/
  src/main/resources/db/migration/
  src/test/java/...
frontend/
  storefront/
  operations-console/
infrastructure/
  docker/ kafka/ observability/ grafana/
docs/
  architecture/ adr/ event-contracts/ incident-runbooks/ plans/ decisions/
tests/
  e2e/ incident-scenarios/
docker-compose.yml
README.md
```

Use package-by-feature inside each backend module. Keep domain logic, application
services, infrastructure adapters, persistence, and API controllers separated.
Do not create circular module dependencies. Do not introduce microservices.

## Local ports and URLs

- Vite runs on the port assigned by the factory orchestrator (`PORT`), default
  `5173`. Storefront: `/`; operations console: `/ops`.
- Spring Boot runs on `PORT + 100`, default `5273`.
- Vite proxies `/api` to `http://localhost:${SERVER_PORT}` so the browser uses
  one origin in local development.
- For factory job `NNN`: builder gets `5100 + 10n`, reviewer-ux `5101 + 10n`,
  reviewer-ui `5102 + 10n` (for example job 001 → Vite 5110, Spring Boot 5210).
- Never hardcode a different port; read it from the environment in both apps.

## Database rules

- One shared local Compose PostgreSQL instance. **One database per job**, named
  `ecom_<job-id>` with non-alphanumeric characters replaced by `_` (for example
  `ecom_001_walking_skeleton`).
- Flyway history lives inside the job database. Migrations are job-scoped.
- A provisioning script creates the job database only if it is absent. It must
  be safe to run repeatedly.
- Never reset the shared cluster, never run `docker compose down -v` on the
  shared project, and never migrate or drop another job's database.
- Integration tests use Testcontainers for isolated databases where practical.
- PostgreSQL is authoritative for durable business state. Redis is never the
  source of truth for orders, payments, or inventory.

## Redis and Kafka policy

- **Redis is not used in job 001.** Add it only when a later job has a concrete
  need, and keep it behind an optional Compose profile.
- **Kafka is not used in job 001.** The shared kind cluster (`kind-kind`) is
  available for later jobs and opt-in smoke checks.
- Kafka host access uses `kubectl port-forward` plus a job-scoped hosts mapping
  to `127.0.0.1` for the advertised broker name. Do not modify the Kafka CR.
  Do not rely on short broker/service names; they do not resolve reliably.
- Integration tests must use Testcontainers or another ephemeral setup. No test
  may require the shared cluster.
- Prefix job topics with the job id or a documented environment prefix.

## Commands

Run these from the repository or the relevant subdirectory once the scaffold
exists. Keep them current when a job adds tooling.

```bash
# Infrastructure
docker compose up -d postgres
docker compose ps
docker compose logs -f postgres

# Backend
./mvnw test
./mvnw verify
./mvnw spring-boot:run

# Frontend
pnpm install
pnpm dev
pnpm test
pnpm build
pnpm exec playwright test

# Full local flow
docker compose up -d postgres
./mvnw spring-boot:run          # or run from backend/
pnpm dev                        # or run from frontend/
```

Do not claim a command passed unless it was actually executed successfully.

## Testing expectations

Every job that changes behavior adds the narrowest useful tests:

- Unit tests for domain logic and order-state transitions.
- Integration tests for repositories, transactions, and migrations.
- API tests for authorization, validation, and error shapes.
- Concurrency tests for inventory reservations.
- Idempotency tests for checkout and payments.
- Playwright end-to-end tests for purchase and administration flows.
- Regression tests for every incident scenario once failure injection exists.

Tests verify business invariants and observable evidence, not just HTTP status
codes. Prefer deterministic fixtures. Do not use arbitrary sleeps as
synchronization.

## Money and business invariants

- Store money as integer cents. Currency is EUR.
- Compute prices, discounts, VAT, and totals on the server. Ignore or reject
  client-submitted prices.
- Persist an order-item snapshot: description, unit price, VAT rate, quantity,
  line total.
- Inventory reservations must never create negative available stock. Use
  optimistic locking or equivalent concurrency control.
- Use an explicit order state machine. No arbitrary status updates.
- Checkout is a saga-style workflow with explicit failure, compensation,
  timeout, bounded retry, and idempotency behavior. Do not pretend PostgreSQL,
  Redis, Kafka, and the payment simulator share one ACID transaction.

## Security hot spots

Every job reviews these explicitly:

- Server-side price and total calculation.
- Inventory invariants under concurrency.
- Authorization on every admin/operations endpoint.
- CSRF for browser state-changing requests; restricted CORS.
- Idempotency keys for checkout, payment, and other critical writes.
- Request validation and safe error responses that do not leak internals.
- Password hashing and session handling (job 002).
- Secrets only through environment variables; never hardcode or commit them.
- No real payment credentials, card data, or financial transactions. The
  payment provider is a deterministic simulator.
- Synthetic data only in seed files and tests.

## Tooling policy

- `pnpm` for JavaScript dependencies and scripts. Keep `packageManager` pinned
  to `pnpm@12.15.0` when the frontend is created.
- Maven Wrapper for the backend. Do not require a system Maven.
- `uv run` / `uvx` only for small Python helpers. Do not introduce Python
  tooling unless a helper genuinely needs it.
- Docker Compose for local infrastructure. Keep it runnable on a developer
  workstation without paid cloud services.
- Do not add production-only dependencies that make local testing difficult.

## Factory and git rules

- Work happens in a sibling worktree:
  `/home/ssura/e-commerce-factory/<job-id>/`, on branch `factory/<job-id>`.
- Never edit the canonical target checkout from a job. The factory merge gate is
  the only path to `main`.
- Never push. Never force-push.
- Treat `factory/jobs/` in the hub as runtime state; it is not part of this repo.
- Keep commits focused and messages descriptive. Do not commit secrets, `.env`,
  build output, or dependency directories.
- Preserve user changes; do not clean up a failed job's worktree or branch.

## Documentation

- Architecture decisions belong in `architecture/adr-*.md`.
- Event contracts and incident runbooks go under `docs/` as later jobs add them.
- Update `README.md` and this file whenever commands, ports, or setup change.
- State which operations are synchronous and which are asynchronous, including
  their consistency guarantees, as the system grows.

## Definition of done for an increment

- The requested user flow works end to end locally.
- Business invariants are enforced in code and covered by tests.
- Failure behavior is explicit; no swallowing, no unbounded retry, no arbitrary
  sleeps, no in-memory durable workflow state.
- Security hot spots above were considered and tested where relevant.
- Documentation and commands match reality.
- The factory review round can run against a clean worktree with the committed
  code.

# CommerceOps — factory bootstrap plan

- **Date:** 2026-10-25
- **Status:** Approved, ready to execute
- **Target alias:** `ecom`
- **Target path:** `/home/ssura/e-commerce-app`
- **Worktree root:** `/home/ssura/e-commerce-app-factory`
- **Hub:** `/home/ssura/software-factory/software-factory-open-code`

## 1. Confirmed product assumptions

- Product name: **CommerceOps**.
- Market: **France/EU**.
- Currency: **EUR**.
- UI language: **French**.
- VAT: configurable **20%** MVP rate.
- First increment: **guest checkout with simulated payment**.
- Accounts and persistent customer order history: **job 002**.
- Kafka/outbox: **job 003**.
- No Kubernetes application deployment yet. Kafka is accessed from the host only
  when a job needs it.

### Server-side money rule

Prices, discounts, VAT, and order totals are always calculated on the server.
Each order persists a snapshot of the purchased item description, unit price,
VAT rate, quantity, and line total. Client-submitted prices are ignored or
rejected and must be covered by tests.

The configured 20% VAT rate is an MVP assumption. It is not a substitute for
validating the tax treatment of real products.

## 2. Confirmed infrastructure decisions

- **Docker Compose + existing kind Kafka** (hybrid).
- Compose runs Postgres for the walking skeleton.
- Redis is **not used in job 001**. Add it only when a later job has a concrete
  need; keep it behind an optional Compose profile.
- Kafka is **not used in job 001**. Kafka connectivity is validated now as an
  environment gate for job 003.
- Kafka integration tests use Testcontainers or another ephemeral setup. The
  shared kind cluster is only for opt-in smoke checks. No test requires the
  shared cluster.
- The walking skeleton does not depend on Redis or Kafka.

### Database isolation

- One shared Compose Postgres instance.
- **One database per job**: `ecom_<job-id>` with non-alphanumeric characters
  replaced by `_`.
- Flyway history lives inside the job database.
- A provisioning script creates the job database only if absent.
- Migrations and test data are job-scoped.
- Never reset the shared database or run `docker compose down -v` on the shared
  project.
- Never allow parallel jobs to reset or migrate each other's databases.
- Integration tests use Testcontainers for isolated databases where practical.

### Kafka access finding

Kafka is healthy, but DNS has a quirk:

- In-cluster topic listing works with the broker FQDN:
  `my-cluster-dual-role-0.my-cluster-kafka-brokers.kafka.svc.cluster.local:9092`
- Short service/broker names do not resolve reliably.
- The broker advertises
  `my-cluster-dual-role-0.my-cluster-kafka-brokers.kafka.svc:9092`, not
  `localhost`.

For job 003, host access uses `kubectl port-forward` plus a job-scoped hosts
mapping to `127.0.0.1` (or a documented `/etc/hosts` entry). Do not modify the
Kafka CR for this. Job 001 is unaffected.

## 3. Environment status

| Item | Status |
|---|---|
| Docker | Working: client/server 29.7.2, Docker Desktop 4.90.0, Compose v5.5.1 |
| Kubernetes | Working: `kubectl` v1.36.1, context `kind-kind`, node Ready |
| Kafka | `my-cluster` Ready (4.2.0), Strimzi operator running |
| pnpm | 12.15.0 installed and pinned |
| agent-browser | 0.39.2 installed |
| Chrome | Chrome for Testing 156.0.8078.47 installed |
| PATH | `.bashrc` exports `PNPM_HOME`; the current non-login shell may need a restart before `agent-browser` is on PATH |
| uv | 0.12.21 available; use for Python helpers only |
| Node | 24.22.1 |
| Java | OpenJDK 21.0.12.1 |
| Maven | 3.9.11 available; use Maven Wrapper in the project |

## 4. Phase 0 — toolchain gates

### pnpm

```bash
corepack enable
corepack prepare pnpm@12.15.0 --activate
pnpm --version   # must print 12.15.0
```

Add `"packageManager": "pnpm@12.15.0"` when the frontend `package.json` is
created.

### agent-browser

Installed binary:

```text
/home/ssura/.local/share/pnpm/bin/agent-browser
```

Verify in a shell where `PNPM_HOME/bin` is on `PATH`:

```bash
agent-browser --version   # must print 0.39.2
```

Install the runtime browser it requires if not already present, then smoke-test
against a local page: navigate, read the title, read page text. This gate must
pass before any UI job.

### Docker

```bash
docker run --rm hello-world
docker compose version
```

Confirm the shared Compose project can start Postgres without touching existing
containers.

### Kafka connectivity

Verify in-cluster topic listing with the broker FQDN. Record the host-access
procedure for job 003. No cluster modification.

### uv

```bash
uv --version
```

Policy:

- `uv run` / `uvx` for Python helpers only.
- `pnpm` for frontend dependencies and scripts.
- Maven Wrapper for the backend.
- Do not introduce Python tooling unless a helper needs it.

### Phase 0 gate

- pnpm pinned to 12.15.0.
- agent-browser 0.39.2 verified with the browser runtime.
- Docker runs a container.
- Kafka reachable in-cluster.
- uv policy documented in the target `AGENTS.md`.

## 5. Phase 1 — repository baseline

Target: `/home/ssura/e-commerce-app`.

Create or replace:

```text
PRODUCT.md                 # vision, market, first increment, exclusions, success metrics
AGENTS.md                  # stack, commands, ports, DB/Redis/Kafka rules, security, uv/pnpm
README.md
.gitignore
.env.example
architecture/
  adr-001-stack-and-module-boundaries.md
  adr-002-identity-auth-and-roles.md
  adr-003-catalog-inventory-cart-orders.md
  adr-004-checkout-payments-idempotency.md
  adr-005-eventing-outbox-kafka.md
  adr-006-observability-and-diagnostics.md
  adr-007-failure-injection.md
docs/decisions/
```

### PRODUCT.md

Keep the CommerceOps vision and scope. Add:

- France/EU, EUR, French UI.
- VAT 20% configurable, with the tax-validation disclaimer.
- Guest-checkout first increment.
- Job 002: customer accounts and order history.
- Job 003: Kafka/outbox.
- Explicit out-of-scope list.
- Success metrics.
- Server-side pricing and order-snapshot rule.

### AGENTS.md

Document:

- Java 21, Spring Boot 4.1.2, Maven Wrapper.
- Postgres 17, React 19, Vite 8, TypeScript 7, pnpm 12.15.0.
- Docker Compose commands and job database naming.
- Redis optional-profile policy.
- Kafka port-forward and topic-prefix policy.
- Port contract: Vite on the orchestrator-assigned `PORT`; Spring Boot on
  `PORT+100`; Vite proxies `/api`.
- Test commands for backend, frontend, and Playwright.
- `uv` for Python helpers; pnpm for JS; Maven Wrapper for Java.
- Synthetic data only.
- Worktree isolation and gitignored build artifacts.
- Security hot spots: server-side price calculation, stock invariants,
  authorization, CSRF, CORS, idempotency, secrets.
- No real payment credentials or real card data.

### ADRs

| ADR | Decision |
|---|---|
| ADR-001 | Stack, module boundaries, repository layout, toolchain, ports |
| ADR-002 | Spring Security, server-side sessions, customer/admin roles (implemented in job 002) |
| ADR-003 | Money in integer cents, EUR, VAT per line, server-side totals, order snapshots, inventory invariants, cart in Postgres for the MVP |
| ADR-004 | Mock payment provider, idempotency keys, reservation/release, compensation |
| ADR-005 | Transactional outbox, topic naming, idempotent consumers, DLQ, no exactly-once claims |
| ADR-006 | Correlation IDs, structured logs, Micrometer/OTel, read-only diagnostic APIs later |
| ADR-007 | Dev/test-only failure injection with authorization, audit, and bounded duration |

### Backlog order

1. Walking skeleton: catalog browse → cart → guest checkout → mock payment →
   order confirmation. Postgres only.
2. Customer accounts, login, account page, order history.
3. Outbox + Kafka order events + idempotent consumers + DLQ.
4. Notification simulator and retries.
5. Observability and diagnostic APIs.
6. Admin/operations console.
7. Failure injection and incident scenarios.
8. Hardening and documentation.

### Git baseline

```bash
git init -b main
git add .
git commit -m "docs: establish CommerceOps product and architecture baseline"
git status --porcelain   # must be empty
```

Phase 1 gate: git repo exists, branch `main`, clean worktree, docs committed.

## 6. Phase 2 — factory registration

Because `opencode.json` currently does not allow the target path, split
registration around the restart.

### Before the restart

1. Add `ecom` to `factory/targets.json`.
2. Add both paths to
   `agent.build.permission.external_directory` in `opencode.json`:
   - `/home/ssura/e-commerce-app/**`
   - `/home/ssura/e-commerce-app-factory/**`
   Keep `"*": "ask"` first and specific allows last.
3. Create `factory/backlog/ecom.md` with the backlog above.
4. Do not modify `factory/merge-gate.sh`, hub `AGENTS.md`, or the dashboard.

### Restart

Restart opencode so the new permissions and target registry are loaded.

### In the fresh session

1. Scaffold and commit the target repository from Phase 1.
2. Run `/factory ecom doctor`.
3. Verify the stop condition.

### Stop condition

Do not launch job 001 until all are true:

- `doctor ecom: OK`
- `git -C /home/ssura/e-commerce-app rev-parse --abbrev-ref HEAD` is `main`
- `git -C /home/ssura/e-commerce-app status --porcelain` is empty
- No job has been created

## 7. First job — walking skeleton

After the stop condition passes:

```text
/factory ecom Build the walking-skeleton storefront defined in PRODUCT.md:
a shopper can browse a seeded catalog, add items to a cart, complete a guest
checkout with the mock payment provider, and see an order confirmation.
Follow architecture/adr-001 through adr-004. Use Postgres via Docker Compose.
Do not add auth, admin UI, Redis, Kafka/outbox, notifications, observability
stack, or failure injection in this increment. Calculate prices and VAT on the
server and persist an order-item snapshot. Use synthetic data only; no real
payment credentials. Update AGENTS.md and README.
```

### In scope

- Catalog browse/search with seeded synthetic products.
- Postgres cart keyed by an anonymous cookie.
- Guest checkout.
- Server-side price/VAT/total calculation.
- Inventory reservation with no negative stock.
- Mock payment provider with deterministic success/failure.
- Order and order-item snapshot.
- Order confirmation.
- French UI, EUR formatting, loading/empty/error/retry states.
- Unit, API integration, concurrency, idempotency, and Playwright tests.

### Out of scope for job 001

- Customer accounts, login, order history.
- Admin UI.
- Redis.
- Kafka/outbox.
- Notifications.
- OTel/Prometheus and diagnostic APIs.
- Operations console.
- Failure injection.
- Kubernetes deployment.

## 8. Merge-gate policy preservation

- Do not modify `factory/merge-gate.sh` or the hub policy surface.
- Edit hub registration files only before any job starts.
- Do not edit `targets.json`, `opencode.json`, or `factory/backlog/ecom.md`
  while a job is running.
- Use explicit `/factory ecom "<feature>"` rather than `/factory next` so the
  backlog is not mutated after `policy_baseline` is recorded.
- Preserve existing dental job state.

## 9. Resume checklist

1. Restart the shell and confirm `agent-browser --version` is 0.39.2.
2. Complete the Phase 0 gates.
3. Build the Phase 1 repository baseline and commit it on `main`.
4. Add the hub registration entries and backlog.
5. Restart opencode.
6. Run `/factory ecom doctor`.
7. Stop until the stop condition is met.
8. Start job 001 with the prompt above.

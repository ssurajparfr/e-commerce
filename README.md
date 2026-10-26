# CommerceOps

CommerceOps is a production-style e-commerce platform for France/EU, designed
for realistic distributed-system behavior and for a later, separate
incident-investigation workflow. The commerce application is built first; the
LangGraph4j + Spring AI investigator is explicitly out of scope for this phase.

- Market: France/EU · Currency: EUR · UI language: French
- First increment: guest checkout with simulated payment
- Java 21 + Spring Boot 4.1.2 backend, React 19 + Vite frontend, PostgreSQL 17

## Status

The repository currently contains the product and architecture baseline:

- `PRODUCT.md` — vision, decided scope, exclusions, success metrics
- `AGENTS.md` — stack, commands, ports, database/Kafka/Redis policy, security
- `architecture/adr-001` … `adr-007` — architecture decisions
- `docs/plans/` — the factory bootstrap plan

Application code is added by factory jobs. The first job is the walking
skeleton: catalog → cart → guest checkout → mock payment → order confirmation,
using PostgreSQL only.

## Quick start

```bash
cp .env.example .env
# Ensure the shared PostgreSQL instance is running on localhost:5432
# with database ecom_001_walking_skeleton.

# Backend (from repo root)
PORT=5110 SERVER_PORT=5210 ./mvnw -pl backend spring-boot:run

# Frontend (from frontend/)
pnpm install
PORT=5110 SERVER_PORT=5210 pnpm dev
```

- Storefront: `http://localhost:5173/`
- Operations console: `http://localhost:5173/ops`
- Backend API: `http://localhost:5273/api`
- Vite proxies `/api` to the backend on `PORT + 100`.

For factory jobs, use the orchestrator-assigned port instead of `5173`; the
backend runs on that port plus 100.

## Tests

```bash
./mvnw test                     # backend
pnpm test                       # frontend unit tests
pnpm exec playwright test       # end-to-end
```

Integration tests use Testcontainers where practical. No test requires the
shared kind Kafka cluster.

## Local infrastructure rules

- One shared Compose PostgreSQL instance; one database per job named
  `ecom_<job-id>`.
- Never run `docker compose down -v` on the shared project.
- Redis is optional and not used by the first increment.
- Kafka is not used by the first increment. Later jobs use the existing
  `kind-kind` cluster through a documented port-forward and host mapping.
- Use synthetic data only. Never use real payment credentials or card data.

## Repository layout (planned)

```text
backend/                Spring Boot modular monolith
frontend/               React storefront + operations console
infrastructure/         Docker, Kafka, observability, Grafana
docs/                   architecture, ADRs, event contracts, runbooks, plans
tests/                  end-to-end and incident scenarios
docker-compose.yml
```

## Factory workflow

This repository is registered as the `ecom` target in the Software Factory hub.
Jobs run in sibling worktrees on `factory/<job-id>` branches and merge only
through the hub's merge gate after review and approval. Never push from a job.

## Documentation

- `PRODUCT.md` — product vision and decided baseline
- `AGENTS.md` — operating instructions for contributors and factory agents
- `architecture/` — decisions that shape the implementation
- `docs/plans/` — bootstrap and delivery plans

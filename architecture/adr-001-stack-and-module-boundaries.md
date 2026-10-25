# ADR-001 — Stack, module boundaries, and repository layout

- **Status:** Accepted
- **Date:** 2026-10-25
- **Deciders:** Product owner + factory bootstrap

## Context

CommerceOps must be a realistic e-commerce platform that can later host an
incident-investigation workflow. The first increment must be a working vertical
slice, not a distributed system operated for appearance. The team is
experienced with Java, Spring, React, PostgreSQL, Redis, Kafka, and distributed
systems.

## Decision

### Stack

- Java 21 (OpenJDK 21.0.x), Spring Boot 4.1.2, Spring MVC, Spring Security,
  Spring Data JPA/Hibernate, Bean Validation, Spring Kafka, Spring Actuator,
  Flyway, OpenAPI, Maven Wrapper 3.9.x.
- React 19.3.1, Vite 8.3.5, TypeScript 7.0.2, React Router, TanStack Query,
  React Hook Form, Tailwind CSS, Vitest, React Testing Library, Playwright.
- PostgreSQL 17 as the authoritative transactional database.
- pnpm 12.15.0, Node 24.x.
- Redis and Kafka are introduced only when a job needs them.
- OpenTelemetry, Prometheus, and Grafana are introduced in the observability job.

These versions were checked against Maven Central and the npm registry on
2026-10-25. If a version is unavailable when a job starts, select the nearest
supported stable release and update this ADR and `AGENTS.md` in the same job.

### Architecture

- Modular monolith, organised package-by-feature.
- Modules: identity, catalog, inventory, cart, checkout, orders, payments,
  notifications, event processing, administration, operations/diagnostics.
- Each module keeps domain logic, application services, infrastructure
  adapters, persistence, and API controllers separated.
- No circular module dependencies. Cross-module calls go through explicit
  application interfaces or domain events.
- Modules are independently testable and should be extractable later without
  requiring extraction now.

### Repository layout

```text
backend/        Spring Boot application + Maven Wrapper
frontend/       storefront and operations console
infrastructure/ Docker, Kafka, observability, Grafana
docs/           architecture, ADRs, event contracts, runbooks, plans
tests/          end-to-end and incident scenarios
docker-compose.yml
```

### Toolchain and ports

- Maven Wrapper for the backend; pnpm for the frontend; `uv` only for Python
  helpers.
- Vite on the orchestrator-assigned `PORT`; Spring Boot on `PORT + 100`; Vite
  proxies `/api` to the backend so the browser sees one origin locally.
- Docker Compose for local infrastructure.

## Consequences

- A single deployable application keeps local development simple while Kafka
  consumers, the payment simulator, Redis, and the outbox still create real
  failure boundaries.
- Package-by-feature keeps future extraction possible without microservice
  overhead today.
- Pinned versions make builds reproducible; version checks at job start prevent
  silent drift.

## Alternatives considered

- Microservices from day one: rejected because it adds operational cost without
  a business need.
- A frontend-only or backend-only slice: rejected because the goal is an
  end-to-end commerce flow with observable evidence.

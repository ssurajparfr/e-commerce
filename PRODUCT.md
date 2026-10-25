# CommerceOps — Production-Style E-commerce Platform

CommerceOps is a production-oriented e-commerce platform for France/EU, designed
from day one to support observability, incident reproduction, distributed
tracing, evidence collection, and controlled operational actions. A later phase
adds a LangGraph4j + Spring AI incident-investigation workflow; that agent is
explicitly out of scope for this phase.

## Decided baseline (2026-10-25)

### Product

- Market: France/EU.
- Currency: EUR.
- UI language: French.
- Product name: CommerceOps.
- VAT: configurable 20% MVP rate. This is an MVP assumption, not a substitute
  for validating the tax treatment of real products.
- First increment (job 001): guest checkout with simulated payment.
- Job 002: customer accounts, login, account page, and persistent order history.
- Job 003: transactional outbox, Kafka order events, idempotent consumers, DLQ.
- No Kubernetes application deployment yet. Kafka is accessed from the host only
  when a job needs it.

### Server-side money rule

Prices, discounts, VAT, and order totals are always calculated on the server.
Each order persists a snapshot of the purchased item description, unit price,
VAT rate, quantity, and line total. Client-submitted prices are ignored or
rejected and must be covered by tests.

### Out of scope for the first increment

Customer accounts and login, admin UI, Redis, Kafka/outbox, notifications,
OpenTelemetry/Prometheus/Grafana, diagnostic APIs, operations console, controlled
failure injection, and Kubernetes deployment. These are planned for later jobs
listed in `factory/backlog/ecom.md` in the factory hub.

### Success metrics for the walking skeleton

- A guest can browse the seeded catalog, add items, check out, pay with the
  mock provider, and see a confirmation in one local flow.
- 100% of checkout totals are computed server-side; tests reject client-submitted
  price tampering.
- Inventory never goes negative under concurrent checkout; the inventory race
  test returns exactly one successful reservation for the last unit.
- Retried checkout requests do not create duplicate orders, payments, or
  inventory reservations.
- All automated tests for the increment pass against a clean database, and no
  real payment credentials or card data exist anywhere in the repository.

### Delivery sequence

1. Walking skeleton: catalog → cart → guest checkout → mock payment → order confirmation. Postgres only.
2. Customer accounts, login, account page, order history.
3. Outbox + Kafka order events + idempotent consumers + DLQ.
4. Notification simulator and retries.
5. Observability and diagnostic APIs.
6. Admin/operations console.
7. Failure injection and incident scenarios.
8. Hardening and documentation.

## Long-form product vision

The sections below are the long-form vision and requirements. They remain
authoritative for later phases; the decided baseline above wins wherever the two
conflict for the current increment.


1. Your Role
Act as a senior software architect, staff-level full-stack engineer, distributed-systems engineer, and DevOps engineer.
Design and implement a realistic, production-oriented e-commerce application with a modern React frontend and a Java Spring Boot backend.
This application will initially operate as a normal e-commerce system. In a later phase, I will build a controlled incident-investigation and remediation workflow using LangGraph4j and Spring AI.
Therefore, the platform must be designed from day one to support observability, incident reproduction, distributed tracing, event inspection, evidence collection, and controlled operational actions.
Do not implement the AI investigation agent in this phase. Build the commerce application and its operational foundations so that the AI workflow can be added later without major architectural changes.
I am experienced with Java, Spring, React, PostgreSQL, Redis, Kafka, and distributed systems. Prioritize sound engineering, realistic business behavior, maintainable architecture, and practical local development over unnecessary abstractions.
2. Primary Objectives
Build an e-commerce platform that supports:
Customer registration, authentication, and account management.
Product catalog, categories, product search, and inventory.
Shopping carts and checkout.
Order creation, order status tracking, and order history.
Payment simulation through a pluggable payment-provider abstraction.
Inventory reservation and release.
Asynchronous order processing through Kafka.
Email and notification simulation.
Administrative operations for products, inventory, orders, and system health.
Operational dashboards and incident-relevant diagnostic information.
The system must exhibit realistic distributed-system behavior, including asynchronous processing, eventual consistency, retries, duplicate message delivery, timeouts, dependency failures, and recovery.
These capabilities must be deterministic and testable where possible. Do not introduce random failures into normal operation.
3. Technology Stack
Use the following stack unless a compelling technical reason requires a documented alternative.
Frontend
React with TypeScript.
Vite.
React Router.
TanStack Query for server state.
A maintainable component library or Tailwind CSS.
React Hook Form with schema-based validation.
A charting library for operational dashboards.
Vitest and React Testing Library.
Playwright for end-to-end tests.
Build a responsive storefront and a separate administrative and operational interface.
Backend
Java 21 or a newer LTS version compatible with the selected Spring Boot release.
Spring Boot 3.x or a newer compatible stable major version.
Spring MVC.
Spring Security.
Spring Data JPA and Hibernate.
Bean Validation.
Spring Kafka.
Spring Actuator.
Spring Boot integration testing support.
Flyway for database migrations.
OpenAPI documentation.
Maven with a Maven Wrapper.
Pin compatible dependency versions and use supported stable releases. Document the selected versions and verify compatibility.
Infrastructure and data
PostgreSQL as the authoritative transactional database.
Redis for cart storage, caching, and explicitly defined temporary state.
Apache Kafka for asynchronous domain events.
Kafka UI for local inspection.
Docker and Docker Compose.
OpenTelemetry for distributed tracing.
Prometheus for metrics.
Grafana for dashboards.
Loki or an equivalent local log aggregation solution if practical.
Prefer infrastructure that can run locally on a developer workstation without requiring paid cloud services.
4. Architecture
Use a modular monolith as the initial application architecture, with well-defined domain modules and independently testable boundaries.
Do not introduce microservices merely for architectural appearance.
Organize the backend around these modules:
Identity and Access
Catalog
Inventory
Cart
Checkout
Orders
Payments
Notifications
Event Processing
Administration
Operations and Diagnostics
Use package-by-feature organization.
Define clear boundaries between domain logic, application services, infrastructure adapters, persistence, and API controllers.
Use domain events for significant business state transitions and Kafka for explicitly asynchronous workflows.
Document which operations are synchronous and which are asynchronous, including their consistency guarantees.
The architecture must permit selected modules to be extracted into separate services in the future without requiring that extraction now.
5. Core Domain Model
Implement realistic entities and relationships.
Customer
ID
Email
Password hash
Name
Account status
Roles
Created and updated timestamps
Product
ID
SKU
Name
Description
Category
Price and currency
Active status
Product metadata
Created and updated timestamps
Inventory
Product ID
Available quantity
Reserved quantity
Version for optimistic concurrency where appropriate
Updated timestamp
Enforce inventory invariants. A reservation must never silently create negative available stock.
Cart
Cart ID
Customer ID or anonymous session reference
Items
Quantities
Last updated timestamp
Support expiration and recovery behavior.
Order
Order ID
Customer ID
Order items
Price snapshots
Subtotal, tax, and total
Currency
Order status
Payment status
Fulfillment status
Idempotency key
Correlation and trace context where appropriate
Created and updated timestamps
Use an explicit order state machine rather than arbitrary status changes.
Example lifecycle:
CREATED → INVENTORY_RESERVED → PAYMENT_PENDING → CONFIRMED → FULFILLMENT_PENDING → SHIPPED → DELIVERED
Also support valid failure and cancellation transitions.
Payment
Payment ID
Order ID
Amount and currency
Provider reference
Payment status
Idempotency key
Failure code
Timestamps
Use a payment simulator, not real payment credentials or real financial transactions.
Operational records
Add structured records for:
Domain event processing attempts.
Failed or dead-lettered messages.
Idempotency keys.
Outbox events.
Administrative actions.
Simulated dependency failures.
Incident test scenarios and execution history.
Keep operational metadata separate from core business entities where appropriate.
6. Business Workflows
Product browsing
Implement product listing, pagination, sorting, filtering, search, category navigation, and product details.
Cart
Support adding, updating, removing, and clearing items.
Define cart expiration behavior and explain how Redis and PostgreSQL interact.
Checkout
Implement an explicit checkout workflow:
Validate the customer and cart.
Validate product prices and availability.
Create a pending order.
Reserve inventory transactionally.
Request payment through the payment-provider abstraction.
Confirm the order after successful payment.
Publish the appropriate domain events.
Notify the customer asynchronously.
Define compensating actions for failure, including releasing reservations and cancelling or marking failed orders when appropriate.
Prevent duplicate orders and duplicate payments when requests are retried.
Do not pretend that PostgreSQL, Redis, Kafka, and an external payment provider participate in a single ACID transaction.
Use a documented saga-style workflow where appropriate.
Implement timeouts, bounded retries, idempotency, and explicit failure states.
Make it possible to inspect the workflow's current state and processing history.
Notifications
Simulate email and order notifications. Persist notification attempts and statuses so failed notifications can be investigated.
Administration
Provide protected screens and APIs to:
Manage products and categories.
Adjust inventory with an audit trail.
Search and inspect orders.
Inspect payment attempts.
View event-processing failures.
Inspect retry and dead-letter activity.
Review application and dependency health.
All administrative mutations must require authorization and produce audit records.
7. Event-Driven Processing
Define versioned Kafka event contracts.
Include events such as:
OrderCreated
InventoryReserved
InventoryReservationFailed
PaymentRequested
PaymentSucceeded
PaymentFailed
OrderConfirmed
OrderCancelled
NotificationRequested
Each event should contain, as appropriate:
Event ID
Event type
Schema version
Aggregate ID
Correlation ID
Causation ID
Occurrence timestamp
Producer identifier
Trace context
Payload
Avoid placing passwords, payment secrets, access tokens, or unnecessary personal information in events and logs.
Implement:
Transactional outbox for reliable database-to-Kafka publication.
Idempotent consumers.
Consumer retry policies.
Dead-letter topics.
Message-processing status and error details.
Duplicate-event detection.
Explicit event schema evolution rules.
Replay procedures that do not duplicate business side effects.
Document delivery semantics and acknowledge that Kafka delivery guarantees alone do not guarantee exactly-once business outcomes across all systems.
Provide scripts or commands for inspecting topics, consumer groups, lag, and failed messages.
8. Observability — A First-Class Requirement
The platform must be designed for future automated incident investigation.
Structured logging
Use JSON logs with consistent fields where applicable:
timestamp
severity
service or module
environment
trace_id
span_id
correlation_id
causation_id
order_id
event_id
operation
outcome
error_code
duration_ms
Use appropriate logging levels. Do not log secrets, credentials, full payment data, or sensitive customer information.
Distributed tracing
Instrument HTTP requests, database access where supported, Kafka producers and consumers, and simulated payment calls using OpenTelemetry.
Propagate trace context across asynchronous boundaries.
Preserve correlation identifiers in event headers and payload metadata where needed.
Metrics
Expose application and infrastructure-relevant metrics, including:
HTTP request rate, latency, and error rate.
Checkout success and failure rates.
Order-processing duration.
Payment success and failure counts.
Inventory reservation failures.
Kafka consumer lag.
Message retry and dead-letter counts.
Notification failures.
Database connection pool utilization.
Redis errors and latency.
Dependency timeouts.
Use bounded-cardinality labels. Never use customer IDs, order IDs, or other unbounded identifiers as Prometheus metric labels.
Health checks
Distinguish liveness, readiness, and dependency health.
Expose useful diagnostic information without exposing internal secrets or sensitive infrastructure details to unauthenticated users.
Diagnostic APIs
Design protected, read-only operational APIs that a future AI investigator can consume.
Examples:
GET /api/ops/health GET /api/ops/dependencies GET /api/ops/orders/{orderId}/timeline GET /api/ops/orders/{orderId}/events GET /api/ops/orders/{orderId}/payment-attempts GET /api/ops/events/{eventId} GET /api/ops/dead-letters GET /api/ops/metrics/summary GET /api/ops/incidents/{incidentId}
Adapt endpoint names where appropriate, but preserve the capabilities.
Provide pagination, time-range filters, access controls, and bounded query sizes.
Prefer structured diagnostic responses over requiring an investigator to parse arbitrary log text.
9. Future AI-Investigation Boundary
Do not implement LangGraph4j, Spring AI, autonomous agents, an LLM client, or agent prompts in this phase.
Instead, establish a clean boundary for a future incident-investigation application.
The future system should be able to:
Receive an incident report.
Identify the affected order, workflow, dependency, or time range.
Gather evidence through approved read-only tools.
Build a timeline from traces, logs, metrics, order states, and Kafka processing records.
Generate evidence-backed hypotheses.
Test hypotheses using additional read-only queries.
Produce a root-cause assessment with confidence and supporting evidence.
Recommend remediation.
Request explicit human approval before any state-changing action.
Execute an allowlisted remediation command.
Verify the outcome and record an audit trail.
Prepare the platform by implementing:
A stable diagnostic API.
Typed response DTOs.
Correlation and trace propagation.
Queryable event and processing history.
An incident scenario catalog.
A remediation-command interface or documented extension point.
An audit model for future investigation and remediation activity.
The future agent must not need direct database credentials, unrestricted Kafka access, shell access, or arbitrary SQL execution.
Design operational APIs around narrow capabilities rather than unrestricted access.
10. Controlled Failure Injection
Implement a development-only failure-injection mechanism to reproduce realistic incidents.
Failure injection must be disabled by default and impossible to enable accidentally in production.
Require an explicit development/test profile, administrative authorization, bounded duration, and an audit record.
Support scenarios such as:
Payment provider timeout.
Payment provider intermittent failure.
Payment provider returning a decline.
Inventory reservation conflict.
Kafka consumer paused or delayed.
Notification provider outage.
Duplicate event delivery.
Message-processing exception.
Redis unavailable.
Increased simulated dependency latency.
Outbox publication delay.
Prefer deterministic, named scenarios over uncontrolled randomness.
For every scenario, define:
Preconditions.
Injection mechanism.
Expected customer-visible symptoms.
Expected logs, metrics, and traces.
Expected business-state transitions.
Recovery behavior.
Verification procedure.
Failure injection must never corrupt unrelated data or bypass core business invariants.
Provide a documented way to disable an active scenario and return the application to normal behavior.
11. Incident Scenario Catalog
Create executable integration tests and documented runbooks for at least these scenarios.
Scenario A: Payment provider timeout
A customer submits checkout. Payment requests time out, retries occur, and the order remains in a well-defined intermediate state or reaches an explicitly defined terminal state.
The investigator should be able to correlate the order, payment attempts, timeout logs, traces, retries, and final state.
Scenario B: Inventory race condition
Two customers attempt to buy the last unit of a product.
Exactly one reservation should succeed. Verify the inventory invariant and the correct order outcomes.
Scenario C: Kafka consumer failure
An order event is published, but processing fails repeatedly and reaches a dead-letter topic.
The failure must be visible through diagnostics, metrics, logs, and processing history.
Scenario D: Duplicate event delivery
Deliver the same business event more than once.
Verify that idempotency prevents duplicate payments, duplicate inventory reservations, and duplicate order confirmations.
Scenario E: Outbox publication delay
Create an order while publication is delayed.
Verify the outbox record, eventual publication, recovery after restart, and correct final business state.
Scenario F: Notification outage
Confirm an order successfully while the notification provider is unavailable.
Verify that notification failure does not accidentally reverse a successful payment or corrupt the order lifecycle.
Scenario G: Redis outage
Simulate a cart/cache dependency outage and verify the documented degradation or recovery behavior.
Do not treat Redis as the authoritative source for orders, payments, or inventory.
Scenario H: Stuck order workflow
Create a reproducible condition in which an order remains in an intermediate state beyond an expected threshold.
Expose enough evidence to distinguish a slow dependency, failed event consumer, and inconsistent workflow state.
For every scenario, include setup instructions, reproduction steps, expected evidence, cleanup instructions, and automated assertions.
12. Security and Reliability
Implement:
Spring Security with role-based access control.
Secure password hashing.
Appropriate session or token-based authentication, with the selected approach documented.
CSRF protection where applicable.
CORS restrictions.
Request validation.
Rate limiting for sensitive endpoints where practical.
Safe error responses.
Database constraints and transactional boundaries.
Optimistic locking or appropriate concurrency control.
Idempotency for critical write operations.
Audit trails for administrative changes.
Secret management through environment variables.
Least-privilege infrastructure credentials.
Explicit timeouts and bounded retries.
Graceful application shutdown.
Database migration and rollback guidance.
Never use real payment processing in the local application.
Never allow the future AI investigator to bypass the normal authorization and validation paths.
13. Frontend Requirements
Build a polished, usable React application with two areas.
Customer storefront
Home page.
Product listing.
Product details.
Search and category filters.
Cart.
Checkout.
Order confirmation.
Order history.
Order details and status timeline.
Login, registration, and account page.
Responsive layout.
Loading, empty, error, and retry states.
Admin and operations console
Overview dashboard.
Product and inventory management.
Order search and detail view.
Order lifecycle timeline.
Payment-attempt viewer.
Event and consumer-processing viewer.
Dead-letter message viewer.
Dependency health.
Metrics and failure summaries.
Development-only failure-scenario controls.
Audit log viewer.
The operations console should make it possible for a human to investigate the same scenarios intended for the future AI investigator.
Provide useful filters by order ID, event ID, correlation ID, status, and time range.
Clearly distinguish real application state from simulated operational state.
Do not create decorative dashboards populated with hardcoded values. All displayed operational data must come from real backend endpoints, apart from clearly labeled demonstration fixtures where explicitly necessary.
14. API and Data Design
Use REST APIs with consistent conventions.
Provide:
OpenAPI specification.
Standard error response format.
Pagination and sorting.
Validation errors with field-level information.
Stable identifiers.
Database indexes for operational queries.
Explicit transaction boundaries.
Flyway migrations.
Seed data for local development.
API integration tests.
Version externally consumed event contracts and document compatibility rules.
Use PostgreSQL for durable business state. Document the precise role of Redis and Kafka.
Do not introduce distributed transactions across PostgreSQL, Redis, Kafka, and the payment simulator.
15. Testing Strategy
Implement:
Unit tests for domain logic and state transitions.
Integration tests for repositories and transaction boundaries.
Kafka integration tests.
API tests for authorization and validation.
Concurrency tests for inventory reservations.
Idempotency tests.
Failure-injection tests.
End-to-end tests for purchase and administration flows.
Regression tests for all incident scenarios.
Prefer Testcontainers for PostgreSQL, Kafka, and Redis integration tests where practical.
Tests must verify business invariants and observable evidence, not merely HTTP status codes.
16. Developer Experience
Provide a complete local development environment.
Required deliverables:
Docker Compose configuration.
Environment variable example file with safe placeholder values.
Database migrations.
Seed data.
Maven Wrapper.
Frontend package scripts and lockfile.
Local startup and shutdown instructions.
Health-check instructions.
Test commands.
Kafka topic initialization.
Observability configuration.
Sample incident reproduction commands.
Troubleshooting guide.
The main development workflow should be straightforward, ideally:
Start infrastructure with Docker Compose.
Start the Spring Boot application.
Start the React application.
Open the storefront and operations console.
Place an order.
Inject a named failure in the development environment.
Inspect the resulting traces, logs, events, and order timeline.
Run the relevant automated tests.
Document resource requirements and offer a lightweight local profile where feasible.
17. Implementation Workflow
Do not generate the entire system as one giant, unverified code dump.
Work incrementally.
Phase 1: Architecture and project foundation
Produce a concise architecture decision record.
Define modules, data ownership, event contracts, and core state machines.
Establish the repository structure.
Set up Docker Compose, PostgreSQL, Redis, Kafka, migrations, and health checks.
Create the Spring Boot and React applications.
Establish build and test automation.
Phase 2: Core commerce
Implement authentication and authorization.
Implement the catalog and inventory.
Implement the cart and checkout.
Implement orders and the payment simulator.
Implement the customer-facing frontend.
Add unit and integration tests.
Phase 3: Reliable event processing
Add domain events and Kafka.
Add the transactional outbox.
Implement idempotent consumers and dead-letter handling.
Implement asynchronous notifications.
Verify recovery and duplicate-delivery behavior.
Phase 4: Observability and operations
Add structured logging and distributed tracing.
Add metrics, dashboards, and health checks.
Implement diagnostic APIs and the operations console.
Add audit trails and operational search.
Phase 5: Failure scenarios
Implement controlled failure injection.
Add the eight incident scenarios.
Add reproducible integration tests and runbooks.
Verify that each scenario produces sufficient evidence for a future investigator.
Phase 6: Hardening
Run the full test suite.
Validate clean startup from an empty database.
Verify migration and restart behavior.
Check security boundaries and failure recovery.
Document known limitations and production-hardening requirements.
At the end of each phase, provide a summary of implemented functionality, important design decisions, files changed, commands executed, test results, and remaining issues.
Do not claim tests passed unless they were actually executed successfully.
18. Engineering Standards
Prefer simple, explicit, maintainable code.
Avoid unnecessary abstractions and speculative frameworks.
Avoid circular module dependencies.
Keep business logic out of controllers.
Use typed DTOs and explicit domain transitions.
Keep database transactions short.
Make failure behavior explicit.
Avoid swallowing exceptions.
Avoid blind, unbounded retries.
Do not use arbitrary sleeps as synchronization mechanisms.
Do not rely on in-memory state for durable workflow progress.
Do not use hardcoded secrets.
Do not use production-only dependencies that make local testing difficult.
Add meaningful tests alongside implementation.
Document significant architecture decisions.
Use consistent naming across API contracts, events, metrics, logs, and database entities.
19. Definition of Done
The project is complete when:
A user can register, browse products, add items to a cart, check out, and view order history.
Inventory and payment behavior preserve business invariants under concurrent requests and retries.
Asynchronous order processing survives duplicate messages and recoverable infrastructure failures.
Outbox publication and dead-letter handling are testable.
Logs, metrics, and traces can be correlated to an order and its processing timeline.
The operations console can display the relevant evidence for every defined incident scenario.
Failure injection is protected and restricted to development/test environments.
All incident scenarios have reproducible instructions and automated assertions.
The application runs locally with documented commands.
A future LangGraph4j + Spring AI investigation application can use documented, read-only diagnostic APIs and narrowly scoped remediation interfaces without accessing databases or infrastructure directly.
20. First Response and Execution
Begin by examining the existing repository, if any.
If the repository is empty, propose the project structure and architecture decisions.
Then implement Phase 1 and verify it before proceeding.
If an architectural decision is ambiguous, choose a sensible default, document it, and continue rather than repeatedly asking for approval.
Prioritize a working, tested vertical slice over scaffolding every module at once.
Keep the future incident-investigation integration in mind throughout the implementation, but do not implement that AI agent yet.
Recommended architecture
This is the architecture I'd aim for before adding LangGraph4j and Spring AI.
React + TypeScript
Storefront · Admin · Operations console
Spring Boot modular monolith
Catalog · Inventory · Checkout · Orders · Payments · Notifications
Diagnostic APIs · Audit · Failure injection · State machines
PostgreSQL
Source of truth
Redis
Cache · Cart
Kafka
Events · Retries · DLQ
Observability layer
OpenTelemetry · Prometheus · Grafana · Structured logs
Later phase — separate investigation application
LangGraph4j + Spring AI
Evidence collection → Hypothesis testing → Root-cause analysis → Human approval → Controlled remediation
Five design decisions that matter most
1. Start with a modular monolith, not microservices
You want distributed-system failure modes, but you don't need to operate ten separate deployable applications to get them.Kafka consumers, the payment simulator, Redis, the outbox, and asynchronous notifications already create meaningfulfailure boundaries.
You can split services later if there is a concrete reason.
2. Make every order reconstructable
This is the most important design requirement for the future investigator.
For any order, you should be able to reconstruct a timeline like:
Time
Event
Outcome
10:00:01
Checkout requested
Accepted
10:00:02
Inventory reserved
Success
10:00:03
Payment requested
Timeout
10:00:05
Payment retry
Timeout
10:00:10
Retry limit reached
Payment pending review
10:00:11
Order workflow updated
Awaiting recovery
These are illustrative events, not real execution results. Your implementation should derive the timeline from actualpersisted workflow transitions, processing attempts, and correlated telemetry.
3. Separate investigation from remediation
When you introduce the AI system, give it narrowly scoped tools rather than unrestricted access to your infrastructure.
Read tools: inspect order timelines, payment attempts, dependency health, consumer failures, and relevant metrics.
Analysis: correlate evidence, test hypotheses, and explain confidence.
Write tools: expose only explicitly approved remediation commands.
Approval: require human authorization before consequential state changes.
Verification: confirm the outcome independently and record the action.
This makes the project a much stronger demonstration of controlled agentic workflows than a chatbot that simplysummarizes logs.
4. Treat observability as application functionality
Don't leave tracing and diagnostic APIs until the end. Add correlation IDs, event IDs, state-transition history, andstructured errors while implementing checkout.
Otherwise, you'll have an application that works but is difficult to investigate.
5. Build deterministic incident scenarios
Your future AI agent needs a repeatable benchmark. Given the same injected incident, it should receive comparableevidence and be evaluated against an expected root cause.
Start with these three particularly valuable scenarios:
Payment timeout: distinguish a dependency failure from an order-processing failure.
Kafka consumer failure: identify retries, consumer lag, and dead-letter evidence.
Inventory race: distinguish a concurrency conflict from an infrastructure outage.
Add the remaining scenarios once the end-to-end workflow works.
Suggested repository structure
text
commerce-ops/
├── frontend/
│   ├── storefront/
│   └── operations-console/
├── backend/
│   └── src/main/java/com/example/commerce/
│       ├── identity/
│       ├── catalog/
│       ├── inventory/
│       ├── cart/
│       ├── checkout/
│       ├── orders/
│       ├── payments/
│       ├── notifications/
│       ├── eventprocessing/
│       ├── administration/
│       └── operations/
├── infrastructure/
│   ├── docker/
│   ├── kafka/
│   ├── observability/
│   └── grafana/
├── docs/
│   ├── architecture/
│   ├── adr/
│   ├── event-contracts/
│   └── incident-runbooks/
├── tests/
│   ├── e2e/
│   └── incident-scenarios/
├── docker-compose.yml
└── README.md
Keep the future AI investigator in a separate application or repository when you reach that phase. It should consume thecommerce platform's diagnostic contracts instead of coupling directly to its internal packages.
How I'd execute this project
I'd use a vertical-slice strategy rather than ask a coding agent to implement the entire prompt in one pass.
Foundation: Spring Boot, React, Docker Compose, migrations, security, and health checks.
First vertical slice: browse a product, reserve stock, create an order, simulate payment, and display the resultingorder timeline.
Reliability: transactional outbox, Kafka consumers, retries, idempotency, and dead-letter handling.
Observability: distributed traces, structured logs, metrics, and diagnostic APIs.
Incident lab: deterministic fault injection and automated incident scenarios.
AI integration: a separate LangGraph4j workflow with read-only investigation tools, explicit approval gates, andallowlisted remediation.

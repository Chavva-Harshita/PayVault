# PayVault

Fault-Tolerant Digital Wallet & Real-Time Financial Ledger System — a microservices
project built with React, Spring Boot, MongoDB, Eureka, and Spring Cloud Gateway.

Full architecture, database design, API contract, and development roadmap live in
`payvault-architecture.md` (shared alongside this codebase). This README covers
day-to-day setup as each phase is built.

## Status

**All 13 phases complete.** The full microservices backend, React
frontend, real-time updates, Docker Compose stack, and automated test
suite (unit + integration tests, including a concurrency test proving the
double-spend guard) are all in place.

## Tech Stack

- **Frontend:** React + Vite, React Router, Axios, Tailwind CSS
- **Backend:** Java 17, Spring Boot 3.3, Spring Cloud 2023.0 (Eureka, Gateway, OpenFeign, LoadBalancer)
- **Database:** MongoDB (one logical database per service)
- **Infra:** Docker / Docker Compose, Maven multi-module build

## Project Structure

```
payvault/
├── pom.xml                  # parent Maven POM
├── eureka-server/           # service registry           (Phase 2)
├── api-gateway/             # single entry point          (Phase 7)
├── auth-service/            # credentials + JWT           (Phase 4)
├── user-service/            # profile data                (Phase 3)
├── wallet-service/          # balance, debit/credit        (Phase 5)
├── transaction-service/     # transfer orchestration + ledger (Phase 6)
└── frontend/                # React + Vite app             (Phase 10)
```

## Prerequisites

- JDK 17+
- Maven 3.9+
- Node.js 18+ and npm (for the frontend, from Phase 10 onward)
- Docker + Docker Compose (runs the entire stack - see below)

## Running Multiple Wallet Service Instances (Phase 8)

Wallet Service can run as two instances sharing the same MongoDB database,
both registering under the same logical `WALLET-SERVICE` name in Eureka:

```
mvn -pl wallet-service -am spring-boot:run                                             # instance 1, :8083
mvn -pl wallet-service -am spring-boot:run -Dspring-boot.run.profiles=dev,instance2     # instance 2, :8093
```

Both should appear under a single `WALLET-SERVICE` entry (with two instances)
on the Eureka dashboard at `:8761`. Every response from either instance
carries an `X-Served-By: wallet-service:<port>` header - calling the same
endpoint repeatedly through transaction-service (which resolves
`WALLET-SERVICE` via Eureka + Spring Cloud LoadBalancer, never a hardcoded
port) should show that header alternating between `8083` and `8093`.

## Real-Time Updates (Phase 11)

The Dashboard connects to a STOMP-over-WebSocket endpoint (`/ws`, proxied
through the Gateway) once a wallet exists, and shows a "Live" badge while
connected. When a transfer you're part of completes - sent by you or from
anyone else - your balance and recent activity update immediately, without
a page refresh.

This is deliberately narrow in scope, per the project's phased approach:

- Only the **Dashboard** page is wired to the socket. The Wallet and
  Transactions pages remain refresh/refetch-based - extending live updates
  to them would just mean subscribing to the same message in more places,
  not new backend work, so it's left as a natural follow-up rather than
  built into every screen up front.
- Delivery is **fire-and-forget**: if a user isn't connected when a
  transfer completes, they simply see the correct state next time they load
  a page via the normal REST endpoints. The socket is a convenience layer
  on top of an already-correct REST API, never the source of truth.
- Authentication happens on the **STOMP CONNECT frame**, not the WebSocket
  handshake itself - a browser platform limitation (no custom headers on a
  WS handshake) that both the Gateway's route and the frontend's socket
  client are built around. See `api-gateway`'s `JwtAuthenticationFilter`
  and `transaction-service`'s `StompAuthChannelInterceptor` for the two
  ends of that design.

## Running Everything with Docker (Phase 12)

The entire stack - MongoDB, all six backend services (including a second
Wallet Service instance for load balancing), and the frontend - runs with
one command:

```bash
cp .env.example .env    # fill in a real JWT_SECRET first
docker compose up --build
```

First build takes a few minutes (each backend service compiles inside its
own Maven container stage). Subsequent runs without code changes are much
faster - use `docker compose up` (no `--build`) once images already exist.

Startup order is handled automatically via healthchecks: MongoDB and
Eureka Server come up first, then the four business services, then the
Gateway, then the frontend - each waits for its dependencies to report
healthy rather than just "started."

Once everything is healthy:

| Service | URL |
|---|---|
| Frontend | http://localhost:5173 |
| API Gateway | http://localhost:8080 |
| Eureka dashboard | http://localhost:8761 |

Check status and logs with:
```bash
docker compose ps
docker compose logs -f api-gateway   # or any other service name
```

Stop everything (keeping data) with `docker compose down`, or wipe the
MongoDB volume too with `docker compose down -v`.

**Note on `VITE_API_BASE_URL` / `FRONTEND_ORIGIN`:** these stay set to
`localhost` even inside `docker-compose.yml`, deliberately. The frontend's
JavaScript runs in your browser, not inside the Docker network - it can
reach the Gateway only via a host-published port (`localhost:8080`), never
via a container hostname like `api-gateway`. Every other inter-service URL
in `docker-compose.yml` (Eureka, MongoDB) uses container hostnames instead,
since those calls happen entirely between containers.

## Running Services Manually (alternative, useful for active development)

Running each service individually with `mvn spring-boot:run` (as shown in
earlier phases of this README's history) is still fully supported and is
often more convenient while actively changing one service's code - you get
faster restarts and don't need to rebuild a Docker image per change.
Start MongoDB via `docker compose up -d mongodb` and run the rest with
Maven directly; see each service's own startup command in the project's
build history, or just:

```bash
mvn -pl <module-name> -am spring-boot:run
```
for `eureka-server`, `user-service`, `wallet-service`, `auth-service`,
`transaction-service`, `api-gateway`, in that order, and `npm run dev`
inside `frontend/` for the UI.

## Testing (Phase 13)

Run a single service's tests:
```bash
mvn -pl wallet-service test
mvn -pl transaction-service test
mvn -pl auth-service test
mvn -pl user-service test
mvn -pl api-gateway test
```
Or the whole suite from the root: `mvn test`.

**What's covered, and why each kind of test exists:**

- **Unit tests** (`WalletServiceTest`, `TransactionServiceTest`, `AuthServiceTest`,
  `JwtServiceTest`, `UserServiceTest`, `JwtValidatorTest`) - mock every
  collaborator (repositories, Feign clients, encoders) and check one
  service's own business rules in isolation: validation, exception
  mapping, idempotency branching, the credit-failure compensation path.
  Fast, no external processes needed.

- **`WalletConcurrencyIT`** (wallet-service) - the direct test of Phase 5's
  core claim. Fires two concurrent ₹8,000 debits at a ₹10,000 balance
  through a **real MongoDB** (via Testcontainers) and asserts exactly one
  succeeds, the other gets `INSUFFICIENT_BALANCE`, and the final balance is
  exactly ₹2,000 - never both succeeding (a double-spend) and never both
  failing. A mocked repository could not prove this: the atomicity
  guarantee being tested is a real MongoDB `findAndModify` behavior, not
  application code.

- **`TransactionTransferIT`** (transaction-service) - a full-flow
  integration test: real HTTP dispatch through MockMvc, real persistence
  to MongoDB (via Testcontainers) for transactions/ledger
  entries/idempotency records, with only the genuinely external calls (to
  user-service and wallet-service) mocked at the Spring bean boundary. It
  proves the whole HTTP-in, MongoDB-out path works together, including
  that retrying a transfer with the same `Idempotency-Key` really does
  skip a second debit end-to-end, not just in a mocked unit test.

**Requires Docker** (Testcontainers needs it to start disposable MongoDB
containers per test run) - `WalletConcurrencyIT` and `TransactionTransferIT`
will fail to start without a working Docker daemon available to the JVM
running the tests.

**Deliberately not covered:** `eureka-server` has no custom logic to unit
test (it's Spring Cloud Netflix Eureka's own code, configured, not
extended). `api-gateway`'s routing and CORS config aren't covered by an
automated test here either - `JwtValidatorTest` covers the one piece of
custom logic that actually matters to get right (token validation), and a
full reactive `WebTestClient` test of the whole filter chain was judged
not worth the added complexity for this project's scope.

## Development Roadmap

See `payvault-architecture.md`, section H, for the full 13-phase breakdown.
Phases 1-12 are complete as of this build:

```
Phase 1  → Project setup                 ✓
Phase 2  → Eureka Server                 ✓
Phase 3  → User Service                  ✓
Phase 4  → Auth Service + JWT            ✓
Phase 5  → Wallet Service                ✓
Phase 6  → Transaction Service           ✓
Phase 7  → API Gateway                   ✓
Phase 8  → Load balancing                ✓
Phase 9  → Fault tolerance               ✓
Phase 10 → Frontend                      ✓
Phase 11 → Real-time updates             ✓
Phase 12 → Docker                        ✓
Phase 13 → Testing                       ✓ (this build)
```

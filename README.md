# Tenpo Challenge API

A reactive Spring Boot 3.5.5 (Java 21, WebFlux + R2DBC) REST API that adds two numbers plus a percentage fetched from a mocked external service, records every API call asynchronously for paginated inspection, and enforces a global rate limit — built for the Tenpo backend technical challenge.

## Architecture

Hexagonal architecture (domain / application ports / infrastructure adapters) with Screaming-architecture, feature-first packages. The domain never imports Spring, Reactor, or persistence types.

```
com.tenpo.challenge
├── calculation      domain, application (ports + service), infrastructure (web adapter, external adapter + retry)
├── history          domain, application (ports + service), infrastructure (web adapter + body capture, persistence adapter)
├── ratelimit        cross-cutting: RateLimitFilter, RateLimitProperties, RateLimitConfig (no domain/application layers)
├── shared           error contract, Problem Details writer/handlers, OpenAPI and clock config
└── TenpoChallengeApplication
```

Two ordered `WebFilter`s wrap every `/api/**` request:

| Order | Filter | Role |
|---|---|---|
| `HIGHEST_PRECEDENCE + 10` | `CallHistoryFilter` | Outermost. Captures request/response bytes, dispatches an async history write after the response completes — including rejections from filters further in the chain. |
| `HIGHEST_PRECEDENCE + 20` | `RateLimitFilter` | Consumes a token from the shared bucket; on denial writes 429 through the already-decorated response so the rejection is captured by history too. |

The reactive boundary stops at the application ports: domain and application code work with plain `Mono`/`Flux` pipelines (`filter`, `map`, `flatMap`, `switchIfEmpty`, `onErrorResume`) and never block. BlockHound is installed for every test run to make that provable rather than assumed — see [Running the tests](#running-the-tests).

## Prerequisites

- Docker and Docker Compose
- Java 21 — only needed for local development/IDE use; the Docker build uses its own JDK image

## Quick start

### Option A — pull the published image (no local build)

Image: [`discarok/tenpo-challenge-api`](https://hub.docker.com/r/discarok/tenpo-challenge-api) on Docker Hub (multi-arch: `linux/amd64`, `linux/arm64`).

```bash
docker compose pull
docker compose up -d
```

### Option B — build locally

```bash
docker compose up --build -d
```

Either way, `db` (PostgreSQL 16) becomes healthy before `api` starts, Flyway migrates the schema on `api` startup, and the API is reachable at `http://localhost:8080`.

Tear down and wipe the database volume with:

```bash
docker compose down -v
```

## Endpoints

### Calculate

```bash
curl -s -X POST http://localhost:8080/api/v1/calculate \
  -H "Content-Type: application/json" \
  -d '{"num1": 5, "num2": 5}'
```

```json
{"num1": 5, "num2": 5, "percentage": 10, "result": 11}
```

### Call history (paginated)

```bash
curl -s "http://localhost:8080/api/v1/history?page=0&size=20"
```

```json
{
  "content": [
    {
      "id": 1,
      "occurredAt": "2026-10-02T12:00:00Z",
      "method": "POST",
      "path": "/api/v1/calculate",
      "requestParams": {"query": {}, "body": {"num1": 5, "num2": 5}},
      "responseStatus": 200,
      "responseBody": {"num1": 5, "num2": 5, "percentage": 10, "result": 11}
    }
  ],
  "page": 0,
  "size": 20,
  "hasNext": false
}
```

### Triggering 429 (rate limit)

The whole `/api/**` surface shares one bucket of 3 requests/minute:

```bash
for i in 1 2 3 4; do
  curl -s -o /dev/null -w "%{http_code}\n" -X POST http://localhost:8080/api/v1/calculate \
    -H "Content-Type: application/json" -d '{"num1": 1, "num2": 1}'
done
```

The 4th line prints `429`, with a `Retry-After` header and a Problem Details body naming the capacity and period.

### Triggering 502 (external provider failure)

```bash
PERCENTAGE_PROVIDER_FAILURE_MODE=unavailable docker compose up -d --force-recreate api
curl -s -X POST http://localhost:8080/api/v1/calculate \
  -H "Content-Type: application/json" -d '{"num1": 5, "num2": 5}'
```

Retries 3 times (short exponential backoff) before returning 502. For the single-attempt 4xx path:

```bash
PERCENTAGE_PROVIDER_FAILURE_MODE=client-error docker compose up -d --force-recreate api
```

Restore normal behavior:

```bash
docker compose up -d --force-recreate api
```

### Validation error (400)

```bash
curl -s -X POST http://localhost:8080/api/v1/calculate \
  -H "Content-Type: application/json" -d '{}'
```

Returns a Problem Details body with an `errors` array listing both missing fields.

### API documentation

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Configuration

All values are overridable by environment variable (Spring relaxed binding).

| Variable | Default | Purpose |
|---|---|---|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `tenpo` | Database name |
| `DB_USER` | `tenpo` | Database user |
| `DB_PASSWORD` | `tenpo` | Database password |
| `PERCENTAGE_PROVIDER_FAILURE_MODE` | `none` | `none` \| `unavailable` (3 attempts, 502) \| `client-error` (1 attempt, 502) |

Non-env-var settings (`application.yml`): `rate-limit.capacity=3`, `rate-limit.refill-tokens=3`, `rate-limit.refill-period=1m`; `history.capture.max-request-bytes=16KB`, `history.capture.max-response-bytes=64KB`.

## Running the tests

```bash
./gradlew test
./gradlew build
```

Integration tests use Testcontainers (PostgreSQL), so Docker must be running. Two Gradle `test` task settings exist specifically for local Colima/non-Docker-Desktop setups:

- `systemProperty("api.version", "1.41")` — Colima's Docker API can report a client/server version negotiation that docker-java rejects as "too old" without pinning a usable API version; this property is a no-op on Docker Desktop/CI (where negotiation already picks a compatible version) and only matters when Testcontainers talks to an older Colima API surface.
- `environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", "/var/run/docker.sock")` — points Testcontainers at the conventional socket path so containers can bind-mount the host Docker socket; harmless on setups that already expose that path.

BlockHound (`blockhound-junit-platform`) installs automatically for the whole suite; `BlockHoundSentinelTest` proves it actually intercepts a blocking call (`Thread.sleep` on a parallel scheduler) rather than silently doing nothing.

JaCoCo generates XML and HTML coverage reports under `build/reports/jacoco/test/` on every `test` run; no coverage gate is enforced.

## Technical decisions

| # | Decision | Why | Tradeoff / rejected |
|---|---|---|---|
| 1 | WebFlux + R2DBC over MVC + virtual threads | The challenge explicitly awards a bonus point for reactive; the use case (one external call, one table) is a natural reactive fit, and Resilience4j/Bucket4j both ship reactive adapters | MVC + Java 21 virtual threads is lower-risk and more mature end to end (JDBC, `Page<T>`, `MockMvc`) but forfeits the bonus. The real risk with WebFlux is an accidental blocking call silently degrading the event loop; BlockHound in every test run turns that risk into a build-time guarantee instead of a runtime hope |
| 2 | Spring Boot 3.5.5 on Java 21 | Deliberately pinned rather than "whatever Initializr gives you today" | Spring Initializr now defaults new projects to Boot 4.x; 3.5.5 was chosen for ecosystem maturity (R2DBC, Resilience4j-reactor, springdoc-webflux) at the time this challenge was built |
| 3 | Gradle, Kotlin DSL | Build-script type safety and IDE support | The wrapper jar is 9.7.1 while `distributionUrl` targets Gradle 8.14.3 — intentional: the wrapper launcher jar version and the distribution it downloads are independent, and 8.14.3 is the version the Boot 3.5.5 plugin was validated against |
| 4 | Flyway over JDBC, startup only | R2DBC has no first-class migration tool; the JDBC driver is only ever used for this one-shot migration step, never for request handling | `spring.flyway.url` needs its own JDBC connection string distinct from the R2DBC one; BlockHound does not flag it because Flyway runs on the main thread during startup, not on a Reactor worker |
| 5 | COUNT-free pagination (`fetch size + 1`, `hasNext`) | One query per page instead of a `COUNT(*)` plus a data query | No `totalElements`/`totalPages` in the response — a deliberate, documented tradeoff over exposing an expensive or eventually-stale total |
| 6 | Global in-memory Bucket4j bucket on `/api/**` | The brief says "the API must support max 3 RPM" with no client-identity mechanism; one shared bucket is the literal reading and needs zero extra infrastructure | Not cluster-safe (single-instance assumption, acceptable for this challenge). `/actuator/**` and the Swagger/OpenAPI endpoints are excluded from the pattern so container healthchecks and API exploration never drain tokens meant for the actual API surface |
| 7 | Retry semantics: 3 total attempts, exponential backoff (200ms, x2, capped 1s), 4xx not retried | "Maximum of 3 attempts" read literally as 3 total attempts (Resilience4j's own `maxAttempts` semantics); malformed-request failures (4xx) will not change on retry, so they fail fast | Both exhaustion paths (3 failed attempts on 5xx/timeout, or the single failed attempt on 4xx) are mapped to the same 502 — the client's own request was valid, this server's upstream is at fault |
| 8 | History records every call, including 429/413 and its own GET calls | "Log every call" read literally; rejections and the history endpoint itself are evidence the system is working, not noise to filter out | The write path never blocks or affects the response: it runs on a bounded elastic scheduler, fire-and-forget, with `onErrorResume` swallowing its own failures. A request body over 16KB is recorded with a `null` body (the oversized payload itself is never kept in memory beyond the cap) |
| 9 | Functional style, no `if`/`else`/ternary/`switch` in main code | Project constraint; branching is expressed as `Mono`/`Flux` operator chains (`filter` + `map` + `defaultIfEmpty` + `flatMap(Function.identity())`) or `Map`-based dispatch | More operators to read per branch point, but every branch is a composable, independently testable pipeline step |
| 10 | No percentage caching | Not requested by the brief; an earlier public version of this challenge template did ask for caching, but this one does not, and caching would also mask the retry/failure-mode demo | Every calculation call fetches (or retries) the percentage fresh |
| 11 | RFC 9457 Problem Details for every error | Native to Spring 6/Boot 3, one shared writer (`ProblemDetailWriter`) used by both the `@RestControllerAdvice` and the two filters that reject before reaching a controller | A generic custom error DTO would have needed its own schema and documentation |
| 12 | Hexagonal boundaries enforced by package structure | Domain and application code for `calculation` and `history` never import Spring/R2DBC/Jackson types; only `infrastructure.adapter.*` does | `ratelimit` is cross-cutting infrastructure with no domain/ports, by design — it has no business rule to isolate |

### Out of scope

- Per-client (IP-based) rate limiting — the brief gives no client-identity mechanism to key on
- Multi-instance/distributed rate limiting (e.g. Redis-backed Bucket4j) — single-instance bucket matches the "no added infra" constraint
- `totalElements`/`totalPages` in the history response — traded for a COUNT-free query
- Percentage caching — not requested, and would interfere with the failure-mode demo
- Authentication/authorization — not part of the brief

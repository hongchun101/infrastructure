# Repository Guidelines

> Multi-module Kotlin / Spring Boot 3.5 backend scaffold under the `com.github.infrastructure` base package. The canonical architecture is documented in `docs/superpowers/specs/2026-06-06-backend-multiservice-scaffold-design.md` — treat that as the source of truth and update it (and the matching plan) whenever you change module shape or non-negotiable conventions.

## Project Overview

A modular monolith backend template built as nine Gradle modules. `app` is the only deployable (`app.jar`, main class `com.github.infrastructure.app.InfrastructureApplicationKt`); the other eight modules (`core`, `security`, `observability`, `alert`, `notification`, `filestore`, `scheduler`, `export`) are libraries that auto-register through Spring Boot's auto-configuration imports.

Tech stack (locked by the scaffold design, do not drift):

| Concern        | Choice                                            |
|----------------|---------------------------------------------------|
| Language       | Kotlin 2.3.0, JVM target 25 (toolchain JDK 25)    |
| Framework      | Spring Boot 3.5.14 (web, security, validation, data-redis, jdbc, actuator, mail) |
| Persistence    | Jimmer 0.10.9 (KSP) on PostgreSQL; schema via Flyway |
| Cache/locks    | Redis (Spring Data Redis) — sessions, idempotency, `lock:job:{code}` SETNX |
| Mail (dev)     | MailHog via docker-compose                       |
| File storage   | Pluggable: local + MinIO 8.6 / OSS / COS / S3     |
| Exports        | Apache POI 5.3 (SXSSF) for XLSX, CSV              |
| Auth           | Bearer UUID tokens in Redis (no JWT)              |
| Observability  | `X-Trace-Id` + MDC + Micrometer Prometheus        |
| OpenAPI        | springdoc-openapi 2.6.0                           |

## Architecture & Data Flow

```
client ──HTTP──▶ TraceIdFilter ──▶ AccessTokenAuthenticationFilter ──▶ IdempotencyFilter ──▶ @RestController
                                                                                                    │
                                                                                                    ▼
                                                                                            @Service (business)
                                                                                                    │
                  ┌─────── direct bean injection ───────┐                                            │
                  ▼                                    ▼                                            ▼
              Repository (Jimmer KSqlClient)      SPI Handler beans                 Spring Application Events
                                                (JobHandler, ExportHandler,        (OperationLogEvent,
                                                 AlertChannel, AlertRuleMatcher,     UserNotificationEvent,
                                                 FileStorage)                        AnnouncementPublishedEvent)
                                                                                                    │
                                                                                                    ▼
                                                                                            @EventListener → async persist
                                                                                            + AlertEventBridge → AlertEvaluationService
```

Key cross-cutting contracts (all from `core`):

- **Response envelope** — every controller body is wrapped by `core/.../web/response/RResponseBodyAdvice.kt` into `R<T>(code: 0, message: "success", data: T)`. Use `R.ok(...)` / `R.error(...)` in code.
- **Errors** — throw `BusinessException(code, message, status)` (subclasses `BadRequestException`, `UnauthorizedException`, `ForbiddenException`, `NotFoundException`, `ConflictException`); `core/.../web/exception/GlobalExceptionHandler.kt` maps it plus Spring MVC exceptions to `R<Nothing>` with `code` defaulting to the HTTP status.
- **Auth context** — `CurrentUserContext.get()` / `require()` returns `AuthenticatedUser(id, username, displayName, roles, permissions, accountType)`. Populated by `security/.../filter/AccessTokenAuthenticationFilter.kt`.
- **Permissions** — `@PreAuthorize("@permissionChecker.has('perm:code')")` using `security/.../permission/PermissionChecker.kt`.
- **Idempotency** — `Idempotency-Key` header honored by `core/.../idempotency/IdempotencyFilter.kt` (Redis `SETNX`, replay/in-progress/payload-mismatch states).
- **Trace** — `observability/.../filter/TraceIdFilter.kt` reads/generates `X-Trace-Id`, echoes it on response, pushes it into MDC and into every `OperationLogEvent`.
- **Pluggability** — register beans of type `JobHandler`, `ExportHandler<P,R>`, `AlertChannel`, `AlertRuleMatcher`, `FileStorage`; the matching `*Registry` discovers them automatically.

## Key Directories

| Module          | Base package                                  | Responsibility                                                                                       |
|-----------------|-----------------------------------------------|------------------------------------------------------------------------------------------------------|
| `app/`          | `com.github.infrastructure.app`               | Only deployable; Spring Boot entry point, business domain, Flyway migrations V1–V17, audit subsystem |
| `core/`         | `com.github.infrastructure.core`              | `R<T>`, `GlobalExceptionHandler`, `BusinessException`, `IdempotencyFilter`, OpenAPI config          |
| `security/`     | `com.github.infrastructure.security`          | `/auth/*`, `/me`, UUID bearer auth, BCrypt, `PermissionChecker`, `LoginRateLimiter`                  |
| `observability/`| `com.github.infrastructure.observability`     | `TraceIdFilter`, Micrometer common tags                                                              |
| `alert/`        | `com.github.infrastructure.alert`             | Rule matching, dispatch (email/Slack/DingTalk/webhook), auto-resolve `JobHandler`                    |
| `notification/` | `com.github.infrastructure.notification`      | `UserNotificationEvent` → inbox persistence, `/api/notifications` CRUD                               |
| `filestore/`    | `com.github.infrastructure.filestore`         | `FileObject` lifecycle, presigned tokens, `/api/files/transfer/**` HMAC streaming proxy              |
| `scheduler/`    | `com.github.infrastructure.scheduler`         | DB-backed cron/fixed-delay engine, Redis-worker locks, `/admin/jobs`                                 |
| `export/`       | `com.github.infrastructure.export`            | `ExportHandler<P,R>` SPI, POI/CSV writers, `UploadClient`, `ExportRunner` `JobHandler`               |
| `docs/superpowers/` | n/a                                      | Superpowers design specs and implementation plans (canonical reference for conventions)             |

`app` is the only module that applies `org.springframework.boot` and owns Flyway. Every other module is a library.

## Development Commands

Local JDK is pinned via `gradle.properties` (`org.gradle.java.installations.paths=D:/lang/jdk25`); the Gradle wrapper resolves JDK 25 through the foojay convention.

```bash
# build everything (compile + tests in all modules)
./gradlew build

# run only tests
./gradlew test

# run a single module's tests
./gradlew :app:test
./gradlew :core:test
./gradlew :security:test

# run a single test class
./gradlew :app:test --tests "com.github.infrastructure.app.ArchitectureTest"

# build the deployable jar (output: app/build/libs/app.jar)
./gradlew :app:bootJar

# start infrastructure dependencies (Postgres 16, Redis 7, MailHog, app)
docker compose up -d
# or for local-only infra (no app service): docker compose -f compose.yml up -d

# PowerShell (Windows) — set JDK 25 for the session, then invoke gradlew
$env:JAVA_HOME='D:\lang\jdk25'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
./gradlew.bat test
```

CI (`.github/workflows/ci.yml`) runs four Gradle steps in order — treat this as the canonical verify path:

1. `./gradlew compileKotlin compileTestKotlin`
2. `./gradlew :app:test --tests "com.github.infrastructure.app.ArchitectureTest"` (ArchUnit, no Docker)
3. `./gradlew :app:test --tests "com.github.infrastructure.app.support.ContainerBackedApplicationContextTest"` (Testcontainers; needs Docker socket)
4. `./gradlew test` with `SPRING_DATASOURCE_*` and `SPRING_DATA_REDIS_*` env wired to the service containers

No lint, format, or coverage plugin is configured. Code style is whatever IntelliJ's Kotlin formatter + `-Xjsr305=strict` produce. Do not introduce detekt/ktlint/spotless/JaCoCo without first proposing it in `docs/superpowers/specs/`.

## Code Conventions & Common Patterns

### Package layout
Mirror the Gradle module: `com.github.infrastructure.<module>.<feature>` (e.g. `com.github.infrastructure.app.audit`, `com.github.infrastructure.security.auth`). Keep feature packages self-contained: `controller/`, `service/`, `repository/`, `entity/`, `config/`, `dto/`, `event/` where applicable.

### Naming
- Class names: `PascalCase` (`AuthController`, `PermissionChecker`, `RedisIdempotencyStore`).
- Auto-configuration class: `<Module>AutoConfiguration`; paired with a `*Properties` config bean (e.g. `SecurityProperties`).
- Repository interfaces: `*Repository` with a Redis/Jimmer implementation suffixed by storage (`RedisTokenSessionRepository`, `DatabaseSecurityUserAccountRepository`). Tests use `InMemory*` variants (e.g. `InMemoryTokenSessionRepository`) — never a mocking library.
- SPI handlers: `JobHandler` (code string), `ExportHandler<P,R>`, `AlertChannel`, `AlertRuleMatcher`, `FileStorage`.
- Test files: `<ClassName>Test.kt`. Use Kotlin backtick method names that describe behavior (`\`login rejects bad password\``).

### HTTP contract
- **No `/api/*` prefix on auth/me/project/announcement/dictionary/operation-logs/alert-* routes** — they live at the root by resource name.
- Filestore, notifications, and export job admin routes **do** use `/api/*` (`/api/files/**`, `/api/notifications`, `/api/admin/export-jobs`) because they are infrastructure endpoints layered on top of the public business API. Don't move them.
- `Authorization: Bearer <accessToken>` for everything except `/auth/login`, `/auth/refresh`, `/actuator/**`, and `/v3/api-docs/**`.

### Errors
Throw `BusinessException` (or subclass) for domain errors. Don't bypass the handler with custom controllers; don't expose internal exception messages — the handler returns generic `internal server error` for unknowns. Validation messages report the first field error as `"<field> <message>"`, else `"bad request"`.

### Async / events
- Audit events: `@OperationLog(module, action, description)` on a controller method → `OperationLogInterceptor` → `OperationLogEvent` (Spring `ApplicationEvent`) → `@Async` `OperationLogRecorder` + `AlertEventBridge`.
- Cross-module fan-out: publish a Spring event (`UserNotificationPublisher.publish(...)`, `AnnouncementService.publish(...)`); subscribe in the owning module's `*Listener`. Never reach across modules by direct service call when an event is the established pattern.
- Scheduled work: implement `JobHandler` and register a bean — `JobRegistry` picks it up; `JobDispatcherService` reconciles `job_definitions`; `JobRunnerService` guards with `lock:job:{code}` SETNX.

### Dependency injection
Constructor injection only, no field injection. Beans are discovered via auto-configuration in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Each library module owns its `*AutoConfiguration`; do not register cross-module beans from `app`.

### Persistence
- Entities are Jimmer `@Entity` classes processed by KSP; repositories implement query methods that return immutable entities or projections. Don't expose entity types from controllers (ArchUnit rule enforces this in `app/.../ArchitectureTest.kt`).
- Flyway migrations live only in `app/src/main/resources/db/migration/` (`V1__init.sql` … `V17__export_permissions.sql`). Test profile uses PostgreSQL-compatible H2 with separate fixtures at `app/src/test/resources/db/migration-test/`.

### State / locks
- All distributed state is in Redis (sessions, idempotency, job worker locks). No in-memory state across replicas.
- `CurrentUserContext` is request-scoped via `SecurityContextHolder`; never store it in a field.

## Important Files

- `app/src/main/kotlin/com/github/infrastructure/app/InfrastructureApplication.kt` — Spring Boot entry point (`@SpringBootApplication(scanBasePackages = ["com.github.infrastructure"])`).
- `app/src/main/resources/application.yml` — central config; every `infrastructure.*` namespace with env-var overrides for containers.
- `app/src/main/resources/logback-spring.xml` — profile-aware logback (human-readable default, JSON in `prod`).
- `app/src/main/resources/db/migration/V1__init.sql` … `V17__export_permissions.sql` — Flyway migrations.
- `app/src/test/kotlin/com/github/infrastructure/app/ArchitectureTest.kt` — ArchUnit layered architecture rules.
- `core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — registers `CoreWebAutoConfiguration`.
- `core/src/main/kotlin/com/github/infrastructure/core/web/response/R.kt` — `R<T>(code, message, data)` envelope.
- `core/src/main/kotlin/com/github/infrastructure/core/web/exception/GlobalExceptionHandler.kt` — error → `R` mapping.
- `core/src/main/kotlin/com/github/infrastructure/core/idempotency/IdempotencyAutoConfiguration.kt` — registers the `IdempotencyFilter`.
- `security/src/main/kotlin/com/github/infrastructure/security/config/SecurityAutoConfiguration.kt` — `@EnableMethodSecurity`, stateless filter chain, public paths.
- `security/src/main/kotlin/com/github/infrastructure/security/permission/PermissionChecker.kt` — used by `@PreAuthorize`.
- `observability/src/main/kotlin/com/github/infrastructure/observability/config/ObservabilityAutoConfiguration.kt` — `TraceIdFilter` + Micrometer common tags.
- `scheduler/src/main/kotlin/com/github/infrastructure/scheduler/service/JobDispatcherService.kt` — 1 s scan loop.
- `export/src/main/kotlin/com/github/infrastructure/export/handler/ExportHandler.kt` — SPI for new export types.
- `Dockerfile` / `docker-compose.yml` — Temurin 25 multi-stage build + Postgres/Redis/MailHog stack.
- `docs/superpowers/specs/2026-06-06-backend-multiservice-scaffold-design.md` — canonical architecture + conventions reference.
- `docs/superpowers/specs/2026-06-03-core-global-exception-handler-design.md` — `R`/`BusinessException`/`GlobalExceptionHandler` design rationale.

## Runtime / Tooling Preferences

- **JDK 25** is mandatory. The Gradle toolchain and `gradle.properties` pin it; the foojay resolver downloads it on CI.
- **Gradle Kotlin DSL** only. `build.gradle.kts`, no Groovy DSL, no `buildSrc` convention plugins.
- **KSP** (not kapt) for Jimmer processing.
- **Repositories** are centrally declared in `settings.gradle.kts` with `repositoriesMode = FAIL_ON_PROJECT_REPOS` — do not add per-module repositories.
- **Conventional Commits** are required (Renovate runs `:semanticCommits`): `build:`, `feat:`, `fix:`, `refactor:`, `test:`, `chore:`, `docs:`. Major versions for Spring Boot, Kotlin, Gradle, and KSP are pinned — Renovate will not bump them; everything else auto-merges on patch.
- **Container images**: production runs as non-root `app` user on Eclipse Temurin 25 JRE Alpine; healthcheck is `GET /actuator/health/liveness`.
- **No scripts directory**, no Makefile/justfile. Gradle wrapper is the only build entry point.

## Testing & QA

- **Frameworks**: JUnit 5 (Jupiter) everywhere, executed via JUnit Platform. AssertJ and MockMvc come from `spring-boot-starter-test`. **No mocking library** (no Mockito, no MockK) — write small in-memory fakes/stubs or use Spring's test slices via real wiring.
- **App-only extras**: ArchUnit 1.4.1 (`ArchitectureTest`), Testcontainers 1.20.4 BOM (PostgreSQL + JUnit Jupiter), embedded Redis 1.4.3 (`EmbeddedRedisTestConfiguration`), H2 (test profile only).
- **Test profile** (`app/src/test/resources/application-test.yml`): PostgreSQL-compatible H2 + Flyway migrations from `classpath:db/migration-test` + embedded Redis on `127.0.0.1:16379`. Test SQL fixtures `V1__init.sql` … `V6__backend_accounts.sql`.
- **Testcontainers profile** (`app/src/test/resources/application-testcontainers.yml`): used by `ContainerBackedApplicationContextTest`. Requires Docker (`DOCKER_HOST=unix:///var/run/docker.sock` in CI).
- **Module coverage**:
  - `app/` — architecture rules, full-context `@SpringBootTest` smoke, authenticated `MockMvc` API integration (`AuthFlowTest`, `DictionaryControllerTest`, …), repository integration against H2 + Flyway.
  - `core/` — servlet-filter unit tests; full-context MVC tests for `RResponseBodyAdvice` and `GlobalExceptionHandler` via nested `@SpringBootApplication`.
  - `security/` — pure service unit tests with deterministic fakes; `WebApplicationContextRunner` auto-config test.
  - `observability/` — `TraceIdFilter` MDC/servlet tests.
  - `scheduler/` — pure cron parser tests.
  - `export/` — pure in-memory CSV/XLSX writer tests.
  - `alert/`, `filestore/`, `notification/` — currently no `src/test`; if you add code, add unit tests with the same in-memory style as the others.
- **No coverage tooling configured**. Don't introduce JaCoCo/Kover without proposing it in `docs/superpowers/specs/` first.
- **Tests must not require Docker** unless they extend `IntegrationTestContainerSupport` and run under the `testcontainers` profile.

## Commit / Contribution Conventions

- Conventional Commits, enforced by Renovate's `:semanticCommits`.
- Branch off `main`; squash-merge via PR (see `.github/workflows/ci.yml` — it runs on `push` and `pull_request` to `main`).
- Reviewer: `hongchun101` (Renovate config).
- When changing non-negotiable conventions (auth model, response envelope, module boundaries, route prefixes), update `docs/superpowers/specs/2026-06-06-backend-multiservice-scaffold-design.md` in the same PR.
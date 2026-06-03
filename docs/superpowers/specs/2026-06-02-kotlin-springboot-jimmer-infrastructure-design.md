# Kotlin Spring Boot Infrastructure Service Design

## Context

The current workspace is empty. This design creates a new Kotlin + Spring Boot service from scratch with Jimmer ORM, PostgreSQL, and Redis.

## Selected approach

Use a complete single-module service skeleton:

- Gradle Kotlin DSL for build configuration.
- Spring Boot 3 on Java 21.
- Kotlin JVM with the Spring compiler plugin and KSP.
- Jimmer SQL Kotlin for ORM and generated DSL/table metadata.
- PostgreSQL as the relational database.
- Redis through Spring Data Redis for cache/connection validation.
- Docker Compose for local PostgreSQL and Redis.
- Flyway for deterministic schema creation.
- A small `Project` domain to prove persistence and cache wiring end-to-end.

This is preferred over a dependency-only skeleton because it gives an executable project and verifies that the main integrations are wired correctly.

## Package and layout

Base package: `com.example.infrastructure`

Expected layout:

```text
.
├── build.gradle.kts
├── settings.gradle.kts
├── compose.yml
├── src/main/kotlin/com/example/infrastructure
│   ├── InfrastructureApplication.kt
│   ├── project
│   │   ├── Project.kt
│   │   ├── ProjectController.kt
│   │   ├── ProjectDtos.kt
│   │   └── ProjectService.kt
│   └── cache
│       └── CacheProbeService.kt
├── src/main/resources
│   ├── application.yml
│   └── db/migration/V1__init.sql
└── src/test/kotlin/com/example/infrastructure
    └── InfrastructureApplicationTests.kt
```

## Runtime behavior

### Project API

The service exposes a minimal project API:

- `POST /api/projects`
  - Request: `{ "name": "example" }`
  - Creates a project row in PostgreSQL using Jimmer.
  - Stores or refreshes a small Redis cache marker for the created project.
  - Response includes project id, name, and created timestamp.

- `GET /api/projects/{id}`
  - Reads the project by id using Jimmer.
  - Returns `404` when no row exists.
  - Touches Redis with a lightweight read/cache probe so Redis integration is exercised without overbuilding cache semantics.

### Persistence

`Project` is a Jimmer Kotlin entity mapped to a PostgreSQL table:

- `id`: UUID primary key.
- `name`: non-empty text.
- `createdTime`: timestamp assigned by the application.

Flyway creates the matching table. The application does not rely on Hibernate DDL generation.

### Redis

Redis is used through Spring Data Redis with `StringRedisTemplate`. The initial design keeps Redis behavior intentionally small: cache a marker or project name by id so the dependency is real and testable without inventing a full cache invalidation policy.

## Configuration

`application.yml` defines:

- Spring datasource settings for PostgreSQL.
- Flyway enabled.
- Redis host/port.
- Jimmer SQL dialect for PostgreSQL.

Local defaults target Docker Compose services:

- PostgreSQL: `localhost:5432`, database `infrastructure`, user `infrastructure`.
- Redis: `localhost:6379`.

## Testing

Tests should prove behavior rather than just context bootstrapping:

- Spring application context loads with project configuration.
- Project creation persists via Jimmer.
- Project lookup returns persisted data.
- Redis interaction is exercised by real Spring wiring where available.

If local Docker services are required for integration tests, tests should clearly rely on the Compose services instead of mocks.

## Non-goals

- Authentication and authorization.
- Multi-module architecture.
- Production deployment manifests.
- Full cache invalidation strategy.
- Business-specific domain beyond the minimal `Project` example.

## Acceptance criteria

- Project can be built with Gradle.
- Application starts with PostgreSQL and Redis available.
- Jimmer generated code is produced through KSP.
- Flyway initializes the PostgreSQL schema.
- Example Project API persists data through PostgreSQL.
- Redis is wired and used by service code.
- Tests cover the example behavior with real application code.

# Backend Multiservice Scaffold Design

## Context

This repository is the base for future backend projects. The current codebase is already moving toward a Kotlin Spring Boot infrastructure template:

- Root Gradle build with Kotlin, Spring Boot, KSP, Jimmer, PostgreSQL, Redis, Flyway, and tests.
- Existing `core` module with unified response `R`, global exception handling, and Web auto-configuration.
- Local `compose.yml` for PostgreSQL and Redis.
- Current application package is `com.github.infrastructure`.

Previous design notes still reference a single-module `com.xample.infrastructure` shape. The scaffold will cut over to the current `com.github.infrastructure` direction and remove that mismatch.

## Selected approach

Use a **single runnable service to start, extensible to multiple backend services**.

Default modules:

- `core`: common Web contract and error handling.
- `security`: Redis-backed session authentication using dual UUID tokens.
- `observability`: trace id, logging context, health/actuator defaults.
- `app`: the default runnable service and example business module.

This is preferred over a two-service template because it gives every new project an immediately runnable backend while keeping the module boundaries needed to add more services later. It is preferred over a platform-library-only skeleton because every project should start from working code, not disconnected libraries.

## Non-negotiable conventions

- Base package: `com.github.infrastructure`.
- Kotlin + Spring Boot with Gradle Kotlin DSL.
- Jimmer for persistence.
- PostgreSQL for relational storage.
- Flyway owns schema creation.
- Redis is part of the default stack because authentication sessions are stored there.
- HTTP routes do **not** use an `/api` prefix. Endpoints are rooted by resource name, such as `/auth/login`, `/auth/refresh`, `/auth/logout`, `/me`, and `/projects`.
- No JWT. Tokens are opaque UUID strings stored server-side in Redis.

## Module responsibilities

### `core`

Owns the HTTP response and error contract shared by all services.

Responsibilities:

- `R<T>` unified response body.
- Automatic wrapping of successful controller responses.
- `BusinessException` for expected business failures.
- `GlobalExceptionHandler` for validation, bad input, missing resources, unsupported methods/media types, authentication/authorization failures, and unexpected errors.
- Auto-configuration so runnable services can depend on `core` without manual bean wiring.

`core` must not depend on project-specific domain code, persistence entities, Redis, or authentication implementation details.

### `security`

Owns authentication, current-user access, and authorization primitives.

Responsibilities:

- Login endpoint support.
- Refresh endpoint support.
- Logout endpoint support.
- Access-token authentication filter.
- Redis token repository.
- Current user context abstraction.
- Password hashing with BCrypt.
- Permission checks for protected endpoints.

The module uses opaque UUID tokens, not self-contained tokens.

Token model:

- `accessToken`: UUID string, short lifetime.
- `refreshToken`: UUID string, longer lifetime.
- Redis stores token session data and expiry.
- Tokens do not embed user data.
- Redis lookup is required for authenticated requests.

Redis session data includes:

- user id
- username
- role codes
- permission codes
- access token id
- refresh token id
- issued time
- expiry time

Authentication flow:

1. `POST /auth/login` receives username and password.
2. Service verifies credentials against PostgreSQL user data.
3. Service generates one access token and one refresh token using UUID values.
4. Service stores both tokens in Redis with their TTLs and linkage.
5. Response returns both token strings and expiration metadata.

Request flow:

1. Client sends `Authorization: Bearer <accessToken>`.
2. Authentication filter reads the token.
3. Filter looks up the token session in Redis.
4. Missing, expired, or invalid tokens return 401 through the common error contract.
5. Valid tokens populate the current user context for controller/service code.

Refresh flow:

1. `POST /auth/refresh` receives a refresh token.
2. Service looks up refresh token state in Redis.
3. Missing or expired refresh token returns 401.
4. Valid refresh rotates both tokens: old access and refresh tokens are deleted, new UUID tokens are stored.
5. Response returns the new token pair.

Logout flow:

1. `POST /auth/logout` requires a valid access token.
2. Service deletes the current access token and linked refresh token from Redis.
3. Subsequent use of either token fails.

Multi-login default:

- Multiple active sessions per user are allowed.
- Each login creates an independent access/refresh token pair.
- Logout invalidates only the current session.
- A later project can add “logout all devices” without changing the basic token model.

### `observability`

Owns operational defaults that every service should inherit.

Responsibilities:

- Request trace id generation and propagation.
- MDC population for logs.
- Response header for trace id.
- Actuator health endpoint defaults.
- Basic request logging that avoids logging passwords or token values.

The trace id contract should be independent from authentication. Anonymous and authenticated requests both receive trace ids.

### `app`

Owns the runnable starter service and example business code.

Responsibilities:

- Spring Boot entry point.
- Depends on `core`, `security`, and `observability`.
- Holds example domain code that proves the scaffold works end-to-end.
- Provides Flyway migrations for default tables.
- Provides configuration for PostgreSQL, Redis, Jimmer, Flyway, Spring Security, and Actuator.

Default routes:

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /me`
- `GET /projects`
- `POST /projects`
- `GET /projects/{id}`

Default routes:

- `POST /auth/login`
- `POST /auth/refresh`
- `POST /auth/logout`
- `GET /me`
- `GET /projects`
- `POST /projects`
- `GET /projects/{id}`
- `GET /dictionaries`, `POST /dictionaries`, `GET /dictionaries/{code}`, `PUT /dictionaries/{id}`, `DELETE /dictionaries/{id}`
- `GET /dictionaries/{code}/items`, `POST /dictionaries/{code}/items`, `PUT /dictionaries/items/{itemId}`, `DELETE /dictionaries/items/{itemId}`
- `GET /announcements`, `POST /announcements`, `GET /announcements/{id}`, `PUT /announcements/{id}`, `POST /announcements/{id}/publish|archive|read|schedule`, `DELETE /announcements/{id}/schedule`
- `GET /login-audits`
- `GET /roles`, `POST /roles`, `GET /roles/{id}`, `PUT /roles/{id}`, `DELETE /roles/{id}`
- `GET /permissions`, `POST /permissions`, `GET /permissions/{id}`, `DELETE /permissions/{id}`
- `PATCH /me`, `POST /me/password`

No route should be introduced under `/api/*`.

## Database model

Flyway initializes the following PostgreSQL tables:

- `users`
- `roles`
- `permissions`
- `user_roles`
- `role_permissions`
- `projects`

`users` fields:

- `id`: UUID primary key.
- `username`: unique non-empty username.
- `password_hash`: BCrypt hash.
- `display_name`: display name.
- `enabled`: whether login is allowed.
- `created_time`: creation timestamp.

`roles` fields:

- `id`: UUID primary key.
- `code`: unique stable role code (uppercase letters / digits / underscore).
- `name`: display name.
- `created_time`: creation timestamp (audit field).

`permissions` fields:

- `id`: UUID primary key.
- `code`: unique stable permission code (lowercase with `:` or `_` separator).
- `name`: display name.
- `description`: optional human-readable description.

`projects` fields:

- `id`: UUID primary key.
- `name`: non-empty project name.
- `owner_id`: user id.
- `created_time`: creation timestamp.

Seed data:

- An enabled administrator user.
- Administrator role.
- Permissions required by the example project endpoints.
- Role-permission links.
- User-role link.

The initial password must be explicit in local configuration or documented test fixtures, not hidden in code comments.

## Cross-cutting business features

Beyond the user/auth/project/announcement surface, `app` ships the following business features that any service backed by this scaffold can rely on.

### Login audit

Every `/auth/login` and `/auth/logout` request is recorded into a partitioned `login_audits` table. The audit row captures:

- `accountType` (USER / BACKEND) and `loginMode` (USERNAME / EMAIL / PHONE) parsed from the request body.
- `principal` and `username` (best-effort, falls back to the request body when the response is a failure).
- `accountId` (when the response carries the userId).
- `outcome` (SUCCESS / FAILURE / LOGOUT) and `failureReason` for failed logins.
- `clientIp`, `userAgent`, `traceId` from MDC.

The audit recording lives in `app/audit/login/LoginAuditRecorder`, a `OncePerRequestFilter` that wraps the request and response with Spring's `ContentCachingRequestWrapper` / `ContentCachingResponseWrapper`. It only runs for `POST /auth/login` and `POST /auth/logout`. This avoids a reverse dependency from `app` to `security` and keeps the security module's `AuthService` unchanged.

`GET /login-audits` (protected by `login:audit:read`) paginates the audit log with filters for `accountType`, `outcome`, `principal`, `accountId`, and time range.

### Role and permission management

`app/user/role` provides full CRUD for roles and permissions plus role-permission binding. New system permissions seeded by `V19__role_permission_management.sql`:

- `role:read`, `role:write` for role CRUD.
- `permission:read`, `permission:write` for permission catalog CRUD.
- `user:profile:write` for self-service profile updates (granted to both ADMIN and BACKEND_OPERATOR).
- `backend:account:read`, `backend:account:update` for backend account management.

Business rules enforced in the service:

- Role `code` is immutable after creation because it is the stable identifier serialized into token sessions and used by downstream code; updates only change `name` and the permission set.
- A role with assigned permissions cannot be deleted (`409 Conflict`); detach first.
- A permission assigned to any role cannot be deleted (`409 Conflict`); detach first.
- Permission IDs in a `CreateRoleRequest` are validated; unknown IDs return `400 Bad Request`.

### Self-service profile

`PATCH /me` updates the current user's display name, email, and phone. `POST /me/password` rotates the password after verifying the current one. Both endpoints are open to any authenticated user (USER or BACKEND) and require no special permission because the only subject is the caller. Uniqueness of email and phone is checked across both account types so that login by principal continues to find a single account.

### Dictionary cache

`app/dictionary/cache/DictionaryCache` provides a Redis-backed read-through cache for the `category by code`, `category by id`, and `items by category` lookups. The cache is invalidated on every write (create / update / delete of categories and items). Cache TTLs are short (`10` minutes for categories, `5` minutes for items) so a stale read window is bounded even if an invalidation is missed.

## HTTP contract

All JSON responses use the shared `R<T>` envelope from `core`.

Successful response shape:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

Error response shape:

```json
{
  "code": 401,
  "message": "unauthorized"
}
```

Authentication endpoints:

`POST /auth/login`

Request:

```json
{
  "username": "admin",
  "password": "admin123"
}
```

Response data:

```json
{
  "accessToken": "uuid-string",
  "refreshToken": "uuid-string",
  "accessTokenExpiresInSeconds": 1800,
  "refreshTokenExpiresInSeconds": 604800
}
```

`POST /auth/refresh`

Request:

```json
{
  "refreshToken": "uuid-string"
}
```

Response data uses the same token-pair shape as login.

`POST /auth/logout`

Requires `Authorization: Bearer <accessToken>` and returns success with empty data.

Current user endpoint:

`GET /me`

Requires authentication and returns user id, username, display name, roles, and permissions.

Project endpoints:

- `GET /projects`: authenticated, requires project read permission.
- `POST /projects`: authenticated, requires project write permission.
- `GET /projects/{id}`: authenticated, requires project read permission.

## Configuration

Default local configuration targets local PostgreSQL and Redis services. `compose.yml` documents matching service defaults for developers who use Compose, but tests must not require Docker.

- PostgreSQL: `localhost:5432`, database `infrastructure`, user `infrastructure`.
- Redis: `localhost:6379`.

Configuration groups:

- `spring.datasource.*`
- `spring.data.redis.*`
- `spring.flyway.*`
- `jimmer.*`
- `management.endpoints.*`
- `infrastructure.security.access-token-ttl`
- `infrastructure.security.refresh-token-ttl`
- `infrastructure.observability.trace-header`

Default token TTLs:

- Access token: 30 minutes.
- Refresh token: 7 days.

## Testing strategy

Tests must prove behavior, not just that the context starts.

Required coverage:

- Context loads for `app` with `core`, `security`, and `observability` auto-configuration.
- Login succeeds with seeded enabled user.
- Login fails with bad password.
- Disabled users cannot log in.
- Authenticated request to `/me` returns the current user.
- Missing access token returns 401 for protected endpoints.
- Invalid access token returns 401.
- Refresh rotates tokens and invalidates the old token pair.
- Logout invalidates the current token pair.
- Project creation persists through Jimmer and PostgreSQL.
- Project read returns persisted data.
- Validation errors use the unified response envelope.
- Trace id is present for successful and failed requests.

Redis must be exercised through real Spring wiring. The scaffold should not mock token storage.

## Migration plan for current repository

The implementation should cut over cleanly from the current shape:

1. Keep and refine the existing `core` module instead of duplicating its response and exception code.
2. Move the runnable application out of the root module into `app`.
3. Add `security` and `observability` modules.
4. Update Gradle settings to include all modules.
5. Align tests and packages to `com.github.infrastructure`.
6. Remove stale `com.example.infrastructure` paths and outdated assumptions from previous notes as affected files are touched.
7. Keep `compose.yml` as optional local service documentation for PostgreSQL and Redis; verification must also work without Docker.

## Non-goals

- JWT support.
- Frontend application or admin UI.
- Service registry, gateway, distributed tracing backend, or Kubernetes manifests.
- Tenant/department/menu management.
- Full audit log system.
- Cross-service RPC examples.
- Complex cache abstraction beyond Redis-backed auth sessions.

## Acceptance criteria

- The repository builds with Gradle.
- `app` starts as the default runnable service.
- PostgreSQL and Redis are represented in `compose.yml` for local development; automated tests use H2 and embedded Redis so verification does not require Docker.
- Flyway creates user, role, permission, and project tables.
- Jimmer entities map to the Flyway schema.
- Production login returns dual UUID tokens stored through the Redis-backed `TokenSessionRepository`; tests verify the same token lifecycle contract through the repository interface without Docker.
- Protected endpoints reject missing or invalid tokens.
- Refresh rotates tokens and invalidates old tokens.
- Logout invalidates the current token pair.
- Routes do not use an `/api` prefix.
- Unified response and exception handling apply to authentication and business endpoints.
- Trace id behavior applies to success and error responses.
- Tests cover the authentication, Redis token lifecycle, persistence, validation, and observability behavior described above.

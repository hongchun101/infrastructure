# Backend Multiservice Scaffold Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Turn this repository into a reusable Kotlin Spring Boot backend scaffold with `core`, `security`, `observability`, and runnable `app` modules.

**Architecture:** Keep the existing `core` Web contract and move the runnable Spring Boot app into `app`. Add `security` for Redis-backed dual UUID token sessions and `observability` for trace id/Actuator defaults. `app` proves the scaffold end-to-end with auth, `/me`, and `/projects` routes without an `/api` prefix.

**Tech Stack:** Kotlin 2.3, Spring Boot 3.5.14, Java toolchain 25, Gradle Kotlin DSL, Jimmer 0.10.9, PostgreSQL, Redis, Flyway, Spring Security, Spring Boot Test, JUnit 5.

---

## File structure

- Modify `settings.gradle.kts`: include `core`, `security`, `observability`, and `app`.
- Replace root `build.gradle.kts`: root aggregator with shared plugin/version declarations and no runnable application code.
- Keep `core/build.gradle.kts`: common web module.
- Create `security/build.gradle.kts`: Spring Security, Web, Data Redis, JDBC/Jimmer-facing abstractions, validation, tests.
- Create `observability/build.gradle.kts`: Web/Actuator/logging module.
- Create `app/build.gradle.kts`: runnable Spring Boot app with Jimmer, KSP, PostgreSQL, Redis, Flyway, and tests.
- Move `src/main/kotlin/com/github/infrastructure/InfrastructureApplication.kt` to `app/src/main/kotlin/com/github/infrastructure/app/InfrastructureApplication.kt`.
- Move `src/main/resources/application.yml` to `app/src/main/resources/application.yml`.
- Replace `src/main/resources/db/migration/V1__init.sql` with `app/src/main/resources/db/migration/V1__init.sql`.
- Move tests from `src/test/...` to `app/src/test/...` and align packages to `com.github.infrastructure.app`.
- Create security files under `security/src/main/kotlin/com/github/infrastructure/security`:
  - `SecurityAutoConfiguration.kt`
  - `SecurityProperties.kt`
  - `AuthenticatedUser.kt`
  - `CurrentUser.kt`
  - `CurrentUserContext.kt`
  - `TokenPair.kt`
  - `TokenSession.kt`
  - `TokenSessionRepository.kt`
  - `RedisTokenSessionRepository.kt`
  - `UuidTokenGenerator.kt`
  - `PasswordHasher.kt`
  - `SecurityUserAccount.kt`
  - `SecurityUserAccountRepository.kt`
  - `AuthService.kt`
  - `AuthController.kt`
  - `AccessTokenAuthenticationFilter.kt`
  - `PermissionChecker.kt`
- Create observability files under `observability/src/main/kotlin/com/github/infrastructure/observability`:
  - `ObservabilityAutoConfiguration.kt`
  - `ObservabilityProperties.kt`
  - `TraceIdFilter.kt`
- Create app files under `app/src/main/kotlin/com/github/infrastructure/app`:
  - `user/User.kt`, `Role.kt`, `Permission.kt`, `UserRole.kt`, `RolePermission.kt`, `DatabaseSecurityUserAccountRepository.kt`
  - `project/Project.kt`, `ProjectDtos.kt`, `ProjectService.kt`, `ProjectController.kt`
- Create auto-configuration imports for `security` and `observability`.

---

### Task 1: Multi-module Gradle cutover

**Files:**
- Modify: `settings.gradle.kts`
- Modify: `build.gradle.kts`
- Create: `security/build.gradle.kts`
- Create: `observability/build.gradle.kts`
- Create: `app/build.gradle.kts`
- Move: `src/main/kotlin/com/github/infrastructure/InfrastructureApplication.kt` → `app/src/main/kotlin/com/github/infrastructure/app/InfrastructureApplication.kt`
- Move: `src/main/resources/application.yml` → `app/src/main/resources/application.yml`
- Move: `src/main/resources/db/migration/V1__init.sql` → `app/src/main/resources/db/migration/V1__init.sql`
- Move: `src/test/resources/application-test.yml` → `app/src/test/resources/application-test.yml`
- Move: `src/test/kotlin/com/example/infrastructure/InfrastructureApplicationTests.kt` → `app/src/test/kotlin/com/github/infrastructure/app/InfrastructureApplicationTests.kt`

- [ ] **Step 1: Write the failing app module context test**

Create `app/src/test/kotlin/com/github/infrastructure/app/InfrastructureApplicationTests.kt`:

```kotlin
package com.github.infrastructure.app

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [InfrastructureApplication::class])
@ActiveProfiles("test")
class InfrastructureApplicationTests {
    @Test
    fun `context loads from app module`() {
    }
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :app:test --tests com.github.infrastructure.app.InfrastructureApplicationTests
```

Expected: FAIL because `:app` is not included yet or `InfrastructureApplication` does not exist under the `app` module.

- [ ] **Step 3: Update module settings**

Replace `settings.gradle.kts` with:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "infrastructure"

include("core")
include("security")
include("observability")
include("app")
```

- [ ] **Step 4: Convert root build to aggregator**

Replace `build.gradle.kts` with:

```kotlin
plugins {
    id("org.springframework.boot") version "3.5.14" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
    kotlin("jvm") version "2.3.0" apply false
    kotlin("plugin.spring") version "2.3.0" apply false
    id("com.google.devtools.ksp") version "2.3.9" apply false
}

group = "com.github"
version = "0.0.1-SNAPSHOT"

allprojects {
    group = rootProject.group
    version = rootProject.version
}
```

- [ ] **Step 5: Create security build file**

Create `security/build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.14")
    }
}

dependencies {
    implementation(project(":core"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_25
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

- [ ] **Step 6: Create observability build file**

Create `observability/build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.14")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_25
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

- [ ] **Step 7: Create app build file**

Create `app/build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    id("com.google.devtools.ksp")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

val jimmerVersion = "0.10.9"

dependencies {
    implementation(project(":core"))
    implementation(project(":security"))
    implementation(project(":observability"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("org.babyfish.jimmer:jimmer-spring-boot-starter:$jimmerVersion") {
        exclude(group = "org.springframework.boot", module = "spring-boot-starter-jdbc")
        exclude(group = "org.springframework.data", module = "spring-data-commons")
    }

    runtimeOnly("org.postgresql:postgresql")

    ksp("org.babyfish.jimmer:jimmer-ksp:$jimmerVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_25
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

- [ ] **Step 8: Move application files**

Create `app/src/main/kotlin/com/github/infrastructure/app/InfrastructureApplication.kt`:

```kotlin
package com.github.infrastructure.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["com.github.infrastructure"])
class InfrastructureApplication

fun main(args: Array<String>) {
    runApplication<InfrastructureApplication>(*args)
}
```

Move existing resources into `app/src/main/resources/` and keep the current `application.yml` content for now.

Move `src/test/resources/application-test.yml` to `app/src/test/resources/application-test.yml`.

Remove the old root `src/` tree after equivalent files exist under `app/`.

- [ ] **Step 9: Verify GREEN**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :app:test --tests com.github.infrastructure.app.InfrastructureApplicationTests
```

Expected: PASS.

- [ ] **Step 10: Commit**

Run:

```powershell
git add settings.gradle.kts build.gradle.kts security/build.gradle.kts observability/build.gradle.kts app src
git commit -m "build: split scaffold into modules"
```

---

### Task 2: Security token session core

**Files:**
- Create: `security/src/test/kotlin/com/github/infrastructure/security/AuthServiceTest.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/SecurityProperties.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/AuthenticatedUser.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/CurrentUser.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/CurrentUserContext.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/TokenPair.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/TokenSession.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/TokenSessionRepository.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/UuidTokenGenerator.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/PasswordHasher.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/SecurityUserAccount.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/SecurityUserAccountRepository.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/AuthService.kt`

- [ ] **Step 1: Write failing auth service tests**

Create `security/src/test/kotlin/com/github/infrastructure/security/AuthServiceTest.kt`:

```kotlin
package com.github.infrastructure.security

import com.github.infrastructure.core.web.BusinessException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.ArrayDeque
import java.util.UUID

class AuthServiceTest {
    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val clock = Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC)
    private val passwordHasher = PlainTestPasswordHasher()

    @Test
    fun `login stores access and refresh tokens with session data`() {
        val tokens = ArrayDeque(listOf("access-1", "refresh-1"))
        val sessionRepository = InMemoryTokenSessionRepository()
        val service = authService(tokens, sessionRepository)

        val response = service.login(LoginRequest("admin", "secret"))

        assertEquals("access-1", response.accessToken)
        assertEquals("refresh-1", response.refreshToken)
        assertEquals(1800, response.accessTokenExpiresInSeconds)
        assertEquals(604800, response.refreshTokenExpiresInSeconds)
        val accessSession = sessionRepository.findAccess("access-1")
        val refreshSession = sessionRepository.findRefresh("refresh-1")
        assertNotNull(accessSession)
        assertNotNull(refreshSession)
        assertEquals(userId, accessSession!!.user.id)
        assertEquals(listOf("ADMIN"), accessSession.user.roles)
        assertEquals(listOf("project:read", "project:write"), accessSession.user.permissions)
        assertEquals("refresh-1", accessSession.refreshToken)
    }

    @Test
    fun `login rejects bad password`() {
        val service = authService(ArrayDeque(listOf("access-1", "refresh-1")), InMemoryTokenSessionRepository())

        val exception = assertThrows(BusinessException::class.java) {
            service.login(LoginRequest("admin", "wrong"))
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
    }

    @Test
    fun `disabled user cannot log in`() {
        val service = authService(
            tokens = ArrayDeque(listOf("access-1", "refresh-1")),
            sessionRepository = InMemoryTokenSessionRepository(),
            enabled = false,
        )

        val exception = assertThrows(BusinessException::class.java) {
            service.login(LoginRequest("admin", "secret"))
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
    }

    @Test
    fun `refresh rotates both tokens and invalidates old pair`() {
        val tokens = ArrayDeque(listOf("access-1", "refresh-1", "access-2", "refresh-2"))
        val sessionRepository = InMemoryTokenSessionRepository()
        val service = authService(tokens, sessionRepository)
        val first = service.login(LoginRequest("admin", "secret"))

        val second = service.refresh(RefreshTokenRequest(first.refreshToken))

        assertEquals("access-2", second.accessToken)
        assertEquals("refresh-2", second.refreshToken)
        assertNotEquals(first.accessToken, second.accessToken)
        assertNotEquals(first.refreshToken, second.refreshToken)
        assertFalse(sessionRepository.hasAccess(first.accessToken))
        assertFalse(sessionRepository.hasRefresh(first.refreshToken))
        assertTrue(sessionRepository.hasAccess(second.accessToken))
        assertTrue(sessionRepository.hasRefresh(second.refreshToken))
    }

    @Test
    fun `logout deletes current token pair`() {
        val serviceRepository = InMemoryTokenSessionRepository()
        val service = authService(ArrayDeque(listOf("access-1", "refresh-1")), serviceRepository)
        val tokens = service.login(LoginRequest("admin", "secret"))

        service.logout(tokens.accessToken)

        assertFalse(serviceRepository.hasAccess(tokens.accessToken))
        assertFalse(serviceRepository.hasRefresh(tokens.refreshToken))
    }

    private fun authService(
        tokens: ArrayDeque<String>,
        sessionRepository: InMemoryTokenSessionRepository,
        enabled: Boolean = true,
    ): AuthService = AuthService(
        userAccountRepository = StaticUserAccountRepository(enabled),
        tokenSessionRepository = sessionRepository,
        tokenGenerator = UuidTokenGenerator { tokens.removeFirst() },
        passwordHasher = passwordHasher,
        properties = SecurityProperties(
            accessTokenTtl = Duration.ofMinutes(30),
            refreshTokenTtl = Duration.ofDays(7),
        ),
        clock = clock,
    )

    private inner class StaticUserAccountRepository(
        private val enabled: Boolean,
    ) : SecurityUserAccountRepository {
        override fun findByUsername(username: String): SecurityUserAccount? =
            SecurityUserAccount(
                id = userId,
                username = username,
                passwordHash = "secret",
                displayName = "Administrator",
                enabled = enabled,
                roles = listOf("ADMIN"),
                permissions = listOf("project:read", "project:write"),
            )
    }

    private class PlainTestPasswordHasher : PasswordHasher {
        override fun matches(rawPassword: String, passwordHash: String): Boolean = rawPassword == passwordHash
    }

    private class InMemoryTokenSessionRepository : TokenSessionRepository {
        private val accessSessions = mutableMapOf<String, TokenSession>()
        private val refreshSessions = mutableMapOf<String, TokenSession>()

        override fun save(session: TokenSession, accessTokenTtl: Duration, refreshTokenTtl: Duration) {
            accessSessions[session.accessToken] = session
            refreshSessions[session.refreshToken] = session
        }

        override fun findByAccessToken(accessToken: String): TokenSession? = accessSessions[accessToken]

        override fun findByRefreshToken(refreshToken: String): TokenSession? = refreshSessions[refreshToken]

        override fun delete(session: TokenSession) {
            accessSessions.remove(session.accessToken)
            refreshSessions.remove(session.refreshToken)
        }

        fun findAccess(token: String): TokenSession? = accessSessions[token]

        fun findRefresh(token: String): TokenSession? = refreshSessions[token]

        fun hasAccess(token: String): Boolean = accessSessions.containsKey(token)

        fun hasRefresh(token: String): Boolean = refreshSessions.containsKey(token)
    }
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :security:test --tests com.github.infrastructure.security.AuthServiceTest
```

Expected: FAIL because the security types do not exist.

- [ ] **Step 3: Implement security core types**

Create `SecurityProperties.kt`:

```kotlin
package com.github.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("infrastructure.security")
data class SecurityProperties(
    val accessTokenTtl: Duration = Duration.ofMinutes(30),
    val refreshTokenTtl: Duration = Duration.ofDays(7),
)
```

Create `AuthenticatedUser.kt`:

```kotlin
package com.github.infrastructure.security

import java.util.UUID

data class AuthenticatedUser(
    val id: UUID,
    val username: String,
    val displayName: String,
    val roles: List<String>,
    val permissions: List<String>,
)
```

Create `CurrentUser.kt`:

```kotlin
package com.github.infrastructure.security

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class CurrentUser
```

Create `CurrentUserContext.kt`:

```kotlin
package com.github.infrastructure.security

import org.springframework.security.core.context.SecurityContextHolder

object CurrentUserContext {
    fun get(): AuthenticatedUser? =
        SecurityContextHolder.getContext().authentication?.principal as? AuthenticatedUser

    fun require(): AuthenticatedUser = get() ?: throw unauthorized()
}
```

Create `TokenPair.kt`:

```kotlin
package com.github.infrastructure.security

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresInSeconds: Long,
    val refreshTokenExpiresInSeconds: Long,
)
```

Create `TokenSession.kt`:

```kotlin
package com.github.infrastructure.security

import java.time.Instant

data class TokenSession(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthenticatedUser,
    val issuedAt: Instant,
    val accessTokenExpiresAt: Instant,
    val refreshTokenExpiresAt: Instant,
)
```

Create `TokenSessionRepository.kt`:

```kotlin
package com.github.infrastructure.security

import java.time.Duration

interface TokenSessionRepository {
    fun save(session: TokenSession, accessTokenTtl: Duration, refreshTokenTtl: Duration)

    fun findByAccessToken(accessToken: String): TokenSession?

    fun findByRefreshToken(refreshToken: String): TokenSession?

    fun delete(session: TokenSession)
}
```

Create `UuidTokenGenerator.kt`:

```kotlin
package com.github.infrastructure.security

import java.util.UUID

class UuidTokenGenerator(
    private val uuid: () -> String = { UUID.randomUUID().toString() },
) {
    fun next(): String = uuid()
}
```

Create `PasswordHasher.kt`:

```kotlin
package com.github.infrastructure.security

interface PasswordHasher {
    fun matches(rawPassword: String, passwordHash: String): Boolean
}
```

Create `SecurityUserAccount.kt`:

```kotlin
package com.github.infrastructure.security

import java.util.UUID

data class SecurityUserAccount(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val displayName: String,
    val enabled: Boolean,
    val roles: List<String>,
    val permissions: List<String>,
)
```

Create `SecurityUserAccountRepository.kt`:

```kotlin
package com.github.infrastructure.security

interface SecurityUserAccountRepository {
    fun findByUsername(username: String): SecurityUserAccount?
}
```

- [ ] **Step 4: Implement auth service and DTOs**

Create `AuthService.kt`:

```kotlin
package com.github.infrastructure.security

import com.github.infrastructure.core.web.BusinessException
import org.springframework.http.HttpStatus
import java.time.Clock
import java.time.Instant

data class LoginRequest(
    val username: String,
    val password: String,
)

data class RefreshTokenRequest(
    val refreshToken: String,
)

class AuthService(
    private val userAccountRepository: SecurityUserAccountRepository,
    private val tokenSessionRepository: TokenSessionRepository,
    private val tokenGenerator: UuidTokenGenerator,
    private val passwordHasher: PasswordHasher,
    private val properties: SecurityProperties,
    private val clock: Clock,
) {
    fun login(request: LoginRequest): TokenPair {
        val account = userAccountRepository.findByUsername(request.username) ?: throw unauthorized()
        if (!account.enabled || !passwordHasher.matches(request.password, account.passwordHash)) {
            throw unauthorized()
        }
        return createSession(account.toAuthenticatedUser())
    }

    fun refresh(request: RefreshTokenRequest): TokenPair {
        val oldSession = tokenSessionRepository.findByRefreshToken(request.refreshToken) ?: throw unauthorized()
        tokenSessionRepository.delete(oldSession)
        return createSession(oldSession.user)
    }

    fun logout(accessToken: String) {
        val session = tokenSessionRepository.findByAccessToken(accessToken) ?: return
        tokenSessionRepository.delete(session)
    }

    fun authenticate(accessToken: String): TokenSession =
        tokenSessionRepository.findByAccessToken(accessToken) ?: throw unauthorized()

    private fun createSession(user: AuthenticatedUser): TokenPair {
        val now = Instant.now(clock)
        val accessToken = tokenGenerator.next()
        val refreshToken = tokenGenerator.next()
        val session = TokenSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user,
            issuedAt = now,
            accessTokenExpiresAt = now.plus(properties.accessTokenTtl),
            refreshTokenExpiresAt = now.plus(properties.refreshTokenTtl),
        )
        tokenSessionRepository.save(session, properties.accessTokenTtl, properties.refreshTokenTtl)
        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresInSeconds = properties.accessTokenTtl.seconds,
            refreshTokenExpiresInSeconds = properties.refreshTokenTtl.seconds,
        )
    }

    private fun SecurityUserAccount.toAuthenticatedUser(): AuthenticatedUser =
        AuthenticatedUser(
            id = id,
            username = username,
            displayName = displayName,
            roles = roles,
            permissions = permissions,
        )
}

fun unauthorized(): BusinessException = BusinessException(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED.value(), "unauthorized")
```

- [ ] **Step 5: Verify GREEN**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :security:test --tests com.github.infrastructure.security.AuthServiceTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add security/src
git commit -m "feat: add token session auth core"
```

---

### Task 3: Redis token repository and security auto-configuration

**Files:**
- Create: `security/src/test/kotlin/com/github/infrastructure/security/SecurityAutoConfigurationTest.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/RedisTokenSessionRepository.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/AuthController.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/AccessTokenAuthenticationFilter.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/PermissionChecker.kt`
- Create: `security/src/main/kotlin/com/github/infrastructure/security/SecurityAutoConfiguration.kt`
- Create: `security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Write failing auto-configuration test**

Create `security/src/test/kotlin/com/github/infrastructure/security/SecurityAutoConfigurationTest.kt`:

```kotlin
package com.github.infrastructure.security

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class SecurityAutoConfigurationTest {
    private val contextRunner = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JacksonAutoConfiguration::class.java, RedisAutoConfiguration::class.java, SecurityAutoConfiguration::class.java))
        .withUserConfiguration(TestAccountConfiguration::class.java)
        .withPropertyValues(
            "spring.data.redis.host=localhost",
            "spring.data.redis.port=6379",
        )

    @Test
    fun `security auto configuration creates auth beans`() {
        contextRunner.run { context ->
            assert(context).hasSingleBean(SecurityProperties::class.java)
            assert(context).hasSingleBean(UuidTokenGenerator::class.java)
            assert(context).hasSingleBean(PasswordHasher::class.java)
            assert(context).hasSingleBean(AuthService::class.java)
            assert(context).hasSingleBean(AuthController::class.java)
            assert(context).hasSingleBean(AccessTokenAuthenticationFilter::class.java)
            assert(context).hasSingleBean(PermissionChecker::class.java)
            assert(context).hasSingleBean(PasswordEncoder::class.java)
        }
    }

    @Configuration(proxyBeanMethods = false)
    private class TestAccountConfiguration {
        @Bean
        fun accountRepository(): SecurityUserAccountRepository = object : SecurityUserAccountRepository {
            override fun findByUsername(username: String): SecurityUserAccount? = SecurityUserAccount(
                id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                username = username,
                passwordHash = "noop",
                displayName = "Test User",
                enabled = true,
                roles = emptyList(),
                permissions = emptyList(),
            )
        }
    }
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :security:test --tests com.github.infrastructure.security.SecurityAutoConfigurationTest
```

Expected: FAIL because auto-configuration types do not exist.

- [ ] **Step 3: Implement Redis token repository**

Create `RedisTokenSessionRepository.kt`:

```kotlin
package com.github.infrastructure.security

import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration

class RedisTokenSessionRepository(
    private val redisTemplate: RedisTemplate<String, TokenSession>,
) : TokenSessionRepository {
    override fun save(session: TokenSession, accessTokenTtl: Duration, refreshTokenTtl: Duration) {
        redisTemplate.opsForValue().set(accessKey(session.accessToken), session, accessTokenTtl)
        redisTemplate.opsForValue().set(refreshKey(session.refreshToken), session, refreshTokenTtl)
    }

    override fun findByAccessToken(accessToken: String): TokenSession? =
        redisTemplate.opsForValue().get(accessKey(accessToken))

    override fun findByRefreshToken(refreshToken: String): TokenSession? =
        redisTemplate.opsForValue().get(refreshKey(refreshToken))

    override fun delete(session: TokenSession) {
        redisTemplate.delete(listOf(accessKey(session.accessToken), refreshKey(session.refreshToken)))
    }

    private fun accessKey(token: String): String = "infrastructure:security:access:$token"

    private fun refreshKey(token: String): String = "infrastructure:security:refresh:$token"
}
```

- [ ] **Step 4: Implement controller and filter**

Create `AuthController.kt`:

```kotlin
package com.github.infrastructure.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/auth/login")
    fun login(@RequestBody request: LoginRequest): TokenPair = authService.login(request)

    @PostMapping("/auth/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequest): TokenPair = authService.refresh(request)

    @PostMapping("/auth/logout")
    fun logout(request: HttpServletRequest) {
        authService.logout(bearerToken(request) ?: throw unauthorized())
    }

    @GetMapping("/me")
    fun me(): AuthenticatedUser = CurrentUserContext.require()
}

fun bearerToken(request: HttpServletRequest): String? {
    val header = request.getHeader("Authorization") ?: return null
    if (!header.startsWith("Bearer ")) {
        return null
    }
    return header.removePrefix("Bearer ").takeIf { it.isNotBlank() }
}
```

Create `AccessTokenAuthenticationFilter.kt`:

```kotlin
package com.github.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class AccessTokenAuthenticationFilter(
    private val authService: AuthService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = bearerToken(request)
        if (token != null) {
            val session = authService.authenticate(token)
            val authorities = session.user.permissions.map(::SimpleGrantedAuthority)
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(session.user, token, authorities)
        }
        filterChain.doFilter(request, response)
    }
}
```

Create `PermissionChecker.kt`:

```kotlin
package com.github.infrastructure.security

import org.springframework.stereotype.Component

@Component("permissionChecker")
class PermissionChecker {
    fun has(permission: String): Boolean = CurrentUserContext.get()?.permissions?.contains(permission) == true
}
```

- [ ] **Step 5: Implement auto-configuration**

Create `SecurityAutoConfiguration.kt`:

```kotlin
package com.github.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.time.Clock

@AutoConfiguration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties::class)
class SecurityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun tokenGenerator(): UuidTokenGenerator = UuidTokenGenerator()

    @Bean
    @ConditionalOnMissingBean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    @ConditionalOnMissingBean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    @ConditionalOnMissingBean
    fun passwordHasher(passwordEncoder: PasswordEncoder): PasswordHasher = object : PasswordHasher {
        override fun matches(rawPassword: String, passwordHash: String): Boolean = passwordEncoder.matches(rawPassword, passwordHash)
    }

    @Bean
    @ConditionalOnMissingBean
    fun tokenSessionRedisTemplate(
        redisConnectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper,
    ): RedisTemplate<String, TokenSession> {
        val template = RedisTemplate<String, TokenSession>()
        template.connectionFactory = redisConnectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = GenericJackson2JsonRedisSerializer(objectMapper)
        template.afterPropertiesSet()
        return template
    }

    @Bean
    @ConditionalOnMissingBean
    fun tokenSessionRepository(redisTemplate: RedisTemplate<String, TokenSession>): TokenSessionRepository = RedisTokenSessionRepository(redisTemplate)

    @Bean
    @ConditionalOnMissingBean
    fun authService(
        userAccountRepository: SecurityUserAccountRepository,
        tokenSessionRepository: TokenSessionRepository,
        tokenGenerator: UuidTokenGenerator,
        passwordHasher: PasswordHasher,
        properties: SecurityProperties,
        clock: Clock,
    ): AuthService = AuthService(userAccountRepository, tokenSessionRepository, tokenGenerator, passwordHasher, properties, clock)

    @Bean
    @ConditionalOnMissingBean
    fun authController(authService: AuthService): AuthController = AuthController(authService)

    @Bean
    @ConditionalOnMissingBean
    fun accessTokenAuthenticationFilter(authService: AuthService): AccessTokenAuthenticationFilter = AccessTokenAuthenticationFilter(authService)

    @Bean
    @ConditionalOnMissingBean
    fun permissionChecker(): PermissionChecker = PermissionChecker()

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        accessTokenAuthenticationFilter: AccessTokenAuthenticationFilter,
    ): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/auth/login", "/auth/refresh", "/actuator/health").permitAll()
            it.anyRequest().authenticated()
        }
        .addFilterBefore(accessTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()
}
```

Create `security/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```text
com.github.infrastructure.security.SecurityAutoConfiguration
```

- [ ] **Step 6: Verify GREEN**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :security:test --tests com.github.infrastructure.security.SecurityAutoConfigurationTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add security/src
git commit -m "feat: add redis security auto configuration"
```

---

### Task 4: Observability module

**Files:**
- Create: `observability/src/test/kotlin/com/github/infrastructure/observability/TraceIdFilterTest.kt`
- Create: `observability/src/main/kotlin/com/github/infrastructure/observability/ObservabilityProperties.kt`
- Create: `observability/src/main/kotlin/com/github/infrastructure/observability/TraceIdFilter.kt`
- Create: `observability/src/main/kotlin/com/github/infrastructure/observability/ObservabilityAutoConfiguration.kt`
- Create: `observability/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`

- [ ] **Step 1: Write failing trace filter tests**

Create `observability/src/test/kotlin/com/github/infrastructure/observability/TraceIdFilterTest.kt`:

```kotlin
package com.github.infrastructure.observability

import jakarta.servlet.FilterChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class TraceIdFilterTest {
    @Test
    fun `uses incoming trace id and writes response header`() {
        val filter = TraceIdFilter(ObservabilityProperties(traceHeader = "X-Trace-Id"))
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        request.addHeader("X-Trace-Id", "trace-123")
        var mdcValue: String? = null

        filter.doFilter(request, response, FilterChain { _, _ -> mdcValue = MDC.get("traceId") })

        assertEquals("trace-123", response.getHeader("X-Trace-Id"))
        assertEquals("trace-123", mdcValue)
        assertEquals(null, MDC.get("traceId"))
    }

    @Test
    fun `generates trace id when request does not provide one`() {
        val filter = TraceIdFilter(ObservabilityProperties(traceHeader = "X-Trace-Id"))
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, FilterChain { _, _ -> assertNotNull(MDC.get("traceId")) })

        val traceId = response.getHeader("X-Trace-Id")
        assertNotNull(traceId)
        assertTrue(traceId.isNotBlank())
    }
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :observability:test --tests com.github.infrastructure.observability.TraceIdFilterTest
```

Expected: FAIL because observability classes do not exist.

- [ ] **Step 3: Implement observability types**

Create `ObservabilityProperties.kt`:

```kotlin
package com.github.infrastructure.observability

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("infrastructure.observability")
data class ObservabilityProperties(
    val traceHeader: String = "X-Trace-Id",
)
```

Create `TraceIdFilter.kt`:

```kotlin
package com.github.infrastructure.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

class TraceIdFilter(
    private val properties: ObservabilityProperties,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = request.getHeader(properties.traceHeader)?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
        response.setHeader(properties.traceHeader, traceId)
        MDC.put("traceId", traceId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove("traceId")
        }
    }
}
```

Create `ObservabilityAutoConfiguration.kt`:

```kotlin
package com.github.infrastructure.observability

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties::class)
class ObservabilityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun traceIdFilter(properties: ObservabilityProperties): TraceIdFilter = TraceIdFilter(properties)
}
```

Create `observability/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`:

```text
com.github.infrastructure.observability.ObservabilityAutoConfiguration
```

- [ ] **Step 4: Verify GREEN**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :observability:test --tests com.github.infrastructure.observability.TraceIdFilterTest
```

Expected: PASS.

- [ ] **Step 5: Commit**

Run:

```powershell
git add observability/src
git commit -m "feat: add trace id observability"
```

---

### Task 5: App database entities and repository wiring

**Files:**
- Create: `app/src/test/kotlin/com/github/infrastructure/app/user/DatabaseSecurityUserAccountRepositoryTest.kt`
- Replace: `app/src/main/resources/db/migration/V1__init.sql`
- Create: `app/src/main/kotlin/com/github/infrastructure/app/user/User.kt`
- Create: `app/src/main/kotlin/com/github/infrastructure/app/user/Role.kt`
- Create: `app/src/main/kotlin/com/github/infrastructure/app/user/Permission.kt`
- Create: `app/src/main/kotlin/com/github/infrastructure/app/user/UserRole.kt`
- Create: `app/src/main/kotlin/com/github/infrastructure/app/user/RolePermission.kt`
- Create: `app/src/main/kotlin/com/github/infrastructure/app/user/DatabaseSecurityUserAccountRepository.kt`

- [ ] **Step 1: Write failing repository test**

Create `app/src/test/kotlin/com/github/infrastructure/app/user/DatabaseSecurityUserAccountRepositoryTest.kt`:

```kotlin
package com.github.infrastructure.app.user

import com.github.infrastructure.app.InfrastructureApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [InfrastructureApplication::class])
@ActiveProfiles("test")
class DatabaseSecurityUserAccountRepositoryTest(
    @Autowired private val repository: DatabaseSecurityUserAccountRepository,
) {
    @Test
    fun `loads seeded admin account with roles and permissions`() {
        val account = repository.findByUsername("admin")

        assertNotNull(account)
        assertEquals("admin", account!!.username)
        assertEquals("Administrator", account.displayName)
        assertEquals(true, account.enabled)
        assertEquals(listOf("ADMIN"), account.roles)
        assertEquals(listOf("project:read", "project:write"), account.permissions)
    }
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :app:test --tests com.github.infrastructure.app.user.DatabaseSecurityUserAccountRepositoryTest
```

Expected: FAIL because user entities and repository do not exist.

- [ ] **Step 3: Replace app test configuration**

Replace `app/src/test/resources/application-test.yml` with:

```yaml
spring:
  flyway:
    enabled: true
  datasource:
    url: jdbc:h2:mem:infrastructure-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE
    username: sa
    password:
    driver-class-name: org.h2.Driver
  data:
    redis:
      repositories:
        enabled: false

jimmer:
  language: kotlin
  dialect: org.babyfish.jimmer.sql.dialect.H2Dialect
  show-sql: true
  pretty-sql: true
```

- [ ] **Step 4: Replace Flyway migration**

Replace `app/src/main/resources/db/migration/V1__init.sql` with:

```sql
create table users (
    id uuid primary key,
    username varchar(100) not null unique,
    password_hash varchar(100) not null,
    display_name varchar(100) not null,
    enabled boolean not null,
    created_time timestamp not null
);

create table roles (
    id uuid primary key,
    code varchar(100) not null unique,
    name varchar(100) not null
);

create table permissions (
    id uuid primary key,
    code varchar(100) not null unique,
    name varchar(100) not null
);

create table user_roles (
    user_id uuid not null references users(id),
    role_id uuid not null references roles(id),
    primary key (user_id, role_id)
);

create table role_permissions (
    role_id uuid not null references roles(id),
    permission_id uuid not null references permissions(id),
    primary key (role_id, permission_id)
);

create table projects (
    id uuid primary key,
    name varchar(200) not null,
    owner_id uuid not null references users(id),
    created_time timestamp not null
);

insert into users (id, username, password_hash, display_name, enabled, created_time) values
('00000000-0000-0000-0000-000000000001', 'admin', '$2a$10$tyl.T7LJM6ZmHqGo/sBc9eDAJq9yRlPKO/L4THUQnQiqYqlHlAhxG', 'Administrator', true, '2026-06-06 00:00:00'),
('00000000-0000-0000-0000-000000000002', 'disabled', '$2a$10$tyl.T7LJM6ZmHqGo/sBc9eDAJq9yRlPKO/L4THUQnQiqYqlHlAhxG', 'Disabled User', false, '2026-06-06 00:00:00');

insert into roles (id, code, name) values
('00000000-0000-0000-0000-000000000101', 'ADMIN', 'Administrator');

insert into permissions (id, code, name) values
('00000000-0000-0000-0000-000000000201', 'project:read', 'Read projects'),
('00000000-0000-0000-0000-000000000202', 'project:write', 'Write projects');

insert into user_roles (user_id, role_id) values
('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000101'),
('00000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000101');

insert into role_permissions (role_id, permission_id) values
('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000201'),
('00000000-0000-0000-0000-000000000101', '00000000-0000-0000-0000-000000000202');
```

The BCrypt hash above matches `admin123`.

- [ ] **Step 5: Create Jimmer entities**

Create `User.kt`:

```kotlin
package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "users")
interface User {
    @Id
    val id: UUID
    val username: String
    val passwordHash: String
    val displayName: String
    val enabled: Boolean
    val createdTime: LocalDateTime
}
```

Create `Role.kt`:

```kotlin
package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.util.UUID

@Entity
@Table(name = "roles")
interface Role {
    @Id
    val id: UUID
    val code: String
    val name: String
}
```

Create `Permission.kt`:

```kotlin
package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.util.UUID

@Entity
@Table(name = "permissions")
interface Permission {
    @Id
    val id: UUID
    val code: String
    val name: String
}
```

Create `UserRole.kt`:

```kotlin
package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table
import java.util.UUID

@Entity
@Table(name = "user_roles")
interface UserRole {
    @Key
    @ManyToOne
    val user: User

    @IdView("user")
    val userId: UUID

    @Key
    @ManyToOne
    val role: Role

    @IdView("role")
    val roleId: UUID
}
```

Create `RolePermission.kt`:

```kotlin
package com.github.infrastructure.app.user

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.Key
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table
import java.util.UUID

@Entity
@Table(name = "role_permissions")
interface RolePermission {
    @Key
    @ManyToOne
    val role: Role

    @IdView("role")
    val roleId: UUID

    @Key
    @ManyToOne
    val permission: Permission

    @IdView("permission")
    val permissionId: UUID
}
```

- [ ] **Step 6: Implement account repository**

Create `DatabaseSecurityUserAccountRepository.kt`:

```kotlin
package com.github.infrastructure.app.user

import com.github.infrastructure.security.SecurityUserAccount
import com.github.infrastructure.security.SecurityUserAccountRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.stereotype.Repository

@Repository
class DatabaseSecurityUserAccountRepository(
    private val sql: KSqlClient,
) : SecurityUserAccountRepository {
    override fun findByUsername(username: String): SecurityUserAccount? {
        val user = sql.createQuery(User::class) {
            where(table.username eq username)
            select(table)
        }.fetchOneOrNull() ?: return null

        val roles = sql.createQuery(UserRole::class) {
            where(table.userId eq user.id)
            select(table.role.code)
        }.execute().sorted()

        val permissions = sql.createQuery(RolePermission::class) {
            where(table.role.code valueIn roles)
            select(table.permission.code)
        }.execute().distinct().sorted()

        return SecurityUserAccount(
            id = user.id,
            username = user.username,
            passwordHash = user.passwordHash,
            displayName = user.displayName,
            enabled = user.enabled,
            roles = roles,
            permissions = permissions,
        )
    }
}
```

- [ ] **Step 7: Verify GREEN**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :app:test --tests com.github.infrastructure.app.user.DatabaseSecurityUserAccountRepositoryTest
```

Expected: PASS.

- [ ] **Step 8: Commit**

Run:

```powershell
git add app/src/main app/src/test
 git commit -m "feat: add scaffold user persistence"
```

---

### Task 6: Project API and no-api route contract

**Files:**
- Create: `app/src/test/kotlin/com/github/infrastructure/app/project/ProjectControllerTest.kt`
- Create: `app/src/main/kotlin/com/github/infrastructure/app/project/Project.kt`
- Create: `app/src/main/kotlin/com/github/infrastructure/app/project/ProjectDtos.kt`
- Create: `app/src/main/kotlin/com/github/infrastructure/app/project/ProjectService.kt`
- Create: `app/src/main/kotlin/com/github/infrastructure/app/project/ProjectController.kt`

- [ ] **Step 1: Write failing project API test**

Create `app/src/test/kotlin/com/github/infrastructure/app/project/ProjectControllerTest.kt`:

```kotlin
package com.github.infrastructure.app.project

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.app.InfrastructureApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.get

@SpringBootTest(classes = [InfrastructureApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProjectControllerTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @Test
    fun `project endpoints are protected and do not use api prefix`() {
        mockMvc.get("/projects")
            .andExpect { status { isUnauthorized() } }

        mockMvc.get("/api/projects")
            .andExpect { status { isNotFound() } }
    }

    @Test
    fun `authenticated user creates and reads project`() {
        val token = login().get("data").get("accessToken").asText()

        val created = mockMvc.post("/projects") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":"base scaffold"}"""
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(0, created.get("code").asInt())
        assertEquals("base scaffold", created.get("data").get("name").asText())
        val projectId = created.get("data").get("id").asText()
        assertTrue(projectId.isNotBlank())

        val fetched = mockMvc.get("/projects/$projectId") {
            header("Authorization", "Bearer $token")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(projectId, fetched.get("data").get("id").asText())
        assertEquals("base scaffold", fetched.get("data").get("name").asText())
    }

    @Test
    fun `validation errors use unified response`() {
        val token = login().get("data").get("accessToken").asText()

        val response = mockMvc.post("/projects") {
            header("Authorization", "Bearer $token")
            contentType = MediaType.APPLICATION_JSON
            content = """{"name":""}"""
        }
            .andExpect { status { isBadRequest() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(400, response.get("code").asInt())
        assertNotNull(response.get("message").asText())
    }

    private fun login(): JsonNode = mockMvc.post("/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"username":"admin","password":"admin123"}"""
    }
        .andExpect { status { isOk() } }
        .andReturn()
        .response
        .contentAsString
        .let(objectMapper::readTree)
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :app:test --tests com.github.infrastructure.app.project.ProjectControllerTest
```

Expected: FAIL because project API does not exist.

- [ ] **Step 3: Implement project entity and DTOs**

Create `Project.kt`:

```kotlin
package com.github.infrastructure.app.project

import com.github.infrastructure.app.user.User
import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.IdView
import org.babyfish.jimmer.sql.ManyToOne
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "projects")
interface Project {
    @Id
    val id: UUID
    val name: String

    @ManyToOne
    val owner: User

    @IdView("owner")
    val ownerId: UUID
    val createdTime: LocalDateTime
}
```

Create `ProjectDtos.kt`:

```kotlin
package com.github.infrastructure.app.project

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import java.util.UUID

data class CreateProjectRequest(
    @field:NotBlank
    val name: String,
)

data class ProjectResponse(
    val id: UUID,
    val name: String,
    val ownerId: UUID,
    val createdTime: LocalDateTime,
)
```

- [ ] **Step 4: Implement project service**

Create `ProjectService.kt`:

```kotlin
package com.github.infrastructure.app.project

import com.github.infrastructure.core.web.BusinessException
import com.github.infrastructure.security.AuthenticatedUser
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class ProjectService(
    private val sql: KSqlClient,
    private val clock: Clock,
) {
    @Transactional
    fun create(request: CreateProjectRequest, user: AuthenticatedUser): ProjectResponse {
        val project = new(Project::class).by {
            id = UUID.randomUUID()
            name = request.name
            ownerId = user.id
            createdTime = LocalDateTime.now(clock)
        }
        sql.save(project)
        return project.toResponse()
    }

    fun list(user: AuthenticatedUser): List<ProjectResponse> = sql.createQuery(Project::class) {
        where(table.ownerId eq user.id)
        orderBy(table.createdTime.desc())
        select(table)
    }.execute().map { it.toResponse() }

    fun get(id: UUID, user: AuthenticatedUser): ProjectResponse {
        val project = sql.createQuery(Project::class) {
            where(table.id eq id)
            where(table.ownerId eq user.id)
            select(table)
        }.fetchOneOrNull() ?: throw BusinessException(HttpStatus.NOT_FOUND, HttpStatus.NOT_FOUND.value(), "resource not found")
        return project.toResponse()
    }

    private fun Project.toResponse(): ProjectResponse = ProjectResponse(
        id = id,
        name = name,
        ownerId = ownerId,
        createdTime = createdTime,
    )
}
```

- [ ] **Step 5: Implement project controller**

Create `ProjectController.kt`:

```kotlin
package com.github.infrastructure.app.project

import com.github.infrastructure.security.CurrentUserContext
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class ProjectController(
    private val projectService: ProjectService,
) {
    @GetMapping("/projects")
    @PreAuthorize("@permissionChecker.has('project:read')")
    fun list(): List<ProjectResponse> = projectService.list(CurrentUserContext.require())

    @PostMapping("/projects")
    @PreAuthorize("@permissionChecker.has('project:write')")
    fun create(@Valid @RequestBody request: CreateProjectRequest): ProjectResponse =
        projectService.create(request, CurrentUserContext.require())

    @GetMapping("/projects/{id}")
    @PreAuthorize("@permissionChecker.has('project:read')")
    fun get(@PathVariable id: UUID): ProjectResponse = projectService.get(id, CurrentUserContext.require())
}
```

- [ ] **Step 6: Verify GREEN**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :app:test --tests com.github.infrastructure.app.project.ProjectControllerTest
```

Expected: PASS.

- [ ] **Step 7: Commit**

Run:

```powershell
git add app/src
 git commit -m "feat: add protected project endpoints"
```

---

### Task 7: End-to-end auth and observability behavior

**Files:**
- Create: `app/src/test/kotlin/com/github/infrastructure/app/auth/AuthFlowTest.kt`
- Modify: `app/src/main/resources/application.yml`

- [ ] **Step 1: Write failing auth flow tests**

Create `app/src/test/kotlin/com/github/infrastructure/app/auth/AuthFlowTest.kt`:

```kotlin
package com.github.infrastructure.app.auth

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.app.InfrastructureApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest(classes = [InfrastructureApplication::class])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowTest(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {
    @Test
    fun `login returns uuid tokens stored server side and me uses access token`() {
        val login = login("admin", "admin123")
        val data = login.get("data")
        val accessToken = data.get("accessToken").asText()
        val refreshToken = data.get("refreshToken").asText()

        assertUuid(accessToken)
        assertUuid(refreshToken)
        assertEquals(1800, data.get("accessTokenExpiresInSeconds").asLong())
        assertEquals(604800, data.get("refreshTokenExpiresInSeconds").asLong())

        val me = mockMvc.get("/me") {
            header("Authorization", "Bearer $accessToken")
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals("admin", me.get("data").get("username").asText())
        assertEquals("ADMIN", me.get("data").get("roles").first().asText())
        assertEquals("project:read", me.get("data").get("permissions").first().asText())
    }

    @Test
    fun `bad credentials disabled users and invalid token return unauthorized envelope`() {
        val badPassword = loginExpectingUnauthorized("admin", "wrong")
        assertEquals(401, badPassword.get("code").asInt())
        assertEquals("unauthorized", badPassword.get("message").asText())

        val disabled = loginExpectingUnauthorized("disabled", "admin123")
        assertEquals(401, disabled.get("code").asInt())

        val invalidToken = mockMvc.get("/me") {
            header("Authorization", "Bearer not-a-real-token")
        }
            .andExpect { status { isUnauthorized() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)

        assertEquals(401, invalidToken.get("code").asInt())
        assertEquals("unauthorized", invalidToken.get("message").asText())
    }

    @Test
    fun `refresh rotates token pair and old tokens stop working`() {
        val first = login("admin", "admin123").get("data")
        val refreshed = mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"${first.get("refreshToken").asText()}"}"""
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response
            .contentAsString
            .let(objectMapper::readTree)
            .get("data")

        assertNotEquals(first.get("accessToken").asText(), refreshed.get("accessToken").asText())
        assertNotEquals(first.get("refreshToken").asText(), refreshed.get("refreshToken").asText())

        mockMvc.get("/me") {
            header("Authorization", "Bearer ${first.get("accessToken").asText()}")
        }.andExpect { status { isUnauthorized() } }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"${first.get("refreshToken").asText()}"}"""
        }.andExpect { status { isUnauthorized() } }

        mockMvc.get("/me") {
            header("Authorization", "Bearer ${refreshed.get("accessToken").asText()}")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `logout invalidates current access and refresh tokens`() {
        val tokens = login("admin", "admin123").get("data")
        val accessToken = tokens.get("accessToken").asText()
        val refreshToken = tokens.get("refreshToken").asText()

        mockMvc.post("/auth/logout") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect { status { isOk() } }

        mockMvc.get("/me") {
            header("Authorization", "Bearer $accessToken")
        }.andExpect { status { isUnauthorized() } }

        mockMvc.post("/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"refreshToken":"$refreshToken"}"""
        }.andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `trace id is present for success and error responses`() {
        val loginResponse = mockMvc.post("/auth/login") {
            header("X-Trace-Id", "trace-from-client")
            contentType = MediaType.APPLICATION_JSON
            content = """{"username":"admin","password":"admin123"}"""
        }
            .andExpect { status { isOk() } }
            .andReturn()
            .response

        assertEquals("trace-from-client", loginResponse.getHeader("X-Trace-Id"))

        val errorResponse = mockMvc.get("/me")
            .andExpect { status { isUnauthorized() } }
            .andReturn()
            .response

        assertNotNull(errorResponse.getHeader("X-Trace-Id"))
    }

    private fun login(username: String, password: String): JsonNode = mockMvc.post("/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"username":"$username","password":"$password"}"""
    }
        .andExpect { status { isOk() } }
        .andReturn()
        .response
        .contentAsString
        .let(objectMapper::readTree)

    private fun loginExpectingUnauthorized(username: String, password: String): JsonNode = mockMvc.post("/auth/login") {
        contentType = MediaType.APPLICATION_JSON
        content = """{"username":"$username","password":"$password"}"""
    }
        .andExpect { status { isUnauthorized() } }
        .andReturn()
        .response
        .contentAsString
        .let(objectMapper::readTree)

    private fun assertUuid(value: String) {
        val uuidPattern = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        assertTrue(uuidPattern.matches(value), "$value is not a UUID")
    }
}
```

- [ ] **Step 2: Verify RED**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :app:test --tests com.github.infrastructure.app.auth.AuthFlowTest
```

Expected: FAIL because full auth HTTP behavior and/or Redis test wiring is incomplete.

- [ ] **Step 3: Update application configuration**

Replace `app/src/main/resources/application.yml` with:

```yaml
spring:
  application:
    name: infrastructure
  datasource:
    url: jdbc:postgresql://localhost:5432/infrastructure
    username: infrastructure
    password: infrastructure
    driver-class-name: org.postgresql.Driver
  data:
    redis:
      host: localhost
      port: 6379
  flyway:
    enabled: true
    locations: classpath:db/migration

jimmer:
  language: kotlin
  dialect: org.babyfish.jimmer.sql.dialect.PostgresDialect
  show-sql: true
  pretty-sql: true

management:
  endpoints:
    web:
      exposure:
        include: health,info

infrastructure:
  security:
    access-token-ttl: 30m
    refresh-token-ttl: 7d
  observability:
    trace-header: X-Trace-Id
```

- [ ] **Step 4: Add Redis test strategy**

Use Spring Boot real Redis wiring in tests without requiring Docker. Add `com.github.codemonstur:embedded-redis:1.4.3` as an app test dependency, start embedded Redis on port `16379` in `EmbeddedRedisTestConfiguration`, and point `application-test.yml` at that port.

Do not replace the production `RedisTokenSessionRepository` with a mock. The auth flow tests must exercise serialization, Redis TTL-backed storage, lookup, refresh rotation, and deletion through the production repository implementation.

- [ ] **Step 5: Verify GREEN**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat :app:test --tests com.github.infrastructure.app.auth.AuthFlowTest
```

Expected: PASS.

- [ ] **Step 6: Commit**

Run:

```powershell
git add app/src
 git commit -m "test: cover auth and observability flows"
```

---

### Task 8: Final scaffold verification and cleanup

**Files:**
- Modify affected docs/spec references only if implementation decisions differ from spec.
- No new feature files unless verification exposes a missing requirement.

- [ ] **Step 1: Verify no stale root source tree**

Check that root `src/` no longer exists or contains no Kotlin/resources used by the build. The runnable application must live under `app/`.

- [ ] **Step 2: Verify no `/api` mappings**

Search source and tests for route mappings containing `/api`. The only acceptable `/api` occurrences are in tests/spec text asserting that `/api/projects` is not found or in documentation that says `/api` is forbidden.

- [ ] **Step 3: Run full tests**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run Gradle configuration gate**

Run:

```powershell
$env:JAVA_HOME='D:\lang\jdk21'; $env:PATH="D:\lang\jdk21\bin;$env:PATH"; ./gradlew.bat tasks
```

Expected: BUILD SUCCESSFUL and `:app`, `:core`, `:security`, and `:observability` tasks are configured.

- [ ] **Step 5: Completion audit**

Audit acceptance criteria from `docs/superpowers/specs/2026-06-06-backend-multiservice-scaffold-design.md`:

- Repository builds with Gradle.
- `app` starts as default runnable service.
- PostgreSQL and Redis are represented in `compose.yml`.
- Flyway creates users/roles/permissions/projects tables.
- Jimmer entities map to schema.
- Login returns dual UUID tokens stored in Redis.
- Protected endpoints reject missing or invalid tokens.
- Refresh rotates and invalidates old tokens.
- Logout invalidates current token pair.
- Routes do not use `/api` prefix.
- Unified response and exception handling apply to auth and project endpoints.
- Trace id applies to success and error responses.
- Tests cover auth, Redis lifecycle, persistence, validation, and observability.

- [ ] **Step 6: Commit final cleanup**

If final cleanup changed files, run the exact command for the touched files. For example, if only this plan and the scaffold spec changed, run:

```powershell
git add docs/superpowers/plans/2026-06-06-backend-multiservice-scaffold.md docs/superpowers/specs/2026-06-06-backend-multiservice-scaffold-design.md
git commit -m "chore: finalize backend scaffold"
```

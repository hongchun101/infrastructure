# Kotlin Spring Boot Jimmer Infrastructure Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a runnable Kotlin Spring Boot service using Jimmer ORM, PostgreSQL, Redis, Docker Compose, Flyway, and behavior tests.

**Architecture:** Create a single-module Spring Boot application under `com.example.infrastructure`. The `project` package owns the example domain, HTTP API, Jimmer persistence, and service transaction boundary; the `cache` package owns the small Redis probe/cache behavior. Flyway owns schema creation so runtime behavior does not depend on generated DDL.

**Tech Stack:** Kotlin JVM, Spring Boot 3.5.x, Java 21, Gradle Kotlin DSL, KSP, Jimmer 0.10.9, PostgreSQL, Redis, Flyway, JUnit 5, Spring Boot Test.

---

## File structure

- Create `settings.gradle.kts`: project name and plugin repositories.
- Create `build.gradle.kts`: Spring Boot, Kotlin, KSP, Jimmer, PostgreSQL, Redis, Flyway, and test dependencies.
- Create `compose.yml`: local PostgreSQL and Redis services.
- Create `src/main/resources/application.yml`: datasource, Redis, Flyway, and Jimmer settings.
- Create `src/main/resources/db/migration/V1__init.sql`: `project` table.
- Create `src/main/kotlin/com/example/infrastructure/InfrastructureApplication.kt`: Spring Boot entry point.
- Create `src/main/kotlin/com/example/infrastructure/project/Project.kt`: Jimmer entity interface.
- Create `src/main/kotlin/com/example/infrastructure/project/ProjectDtos.kt`: request/response DTOs.
- Create `src/main/kotlin/com/example/infrastructure/cache/CacheProbeService.kt`: Redis string cache operations.
- Create `src/main/kotlin/com/example/infrastructure/project/ProjectService.kt`: transactional create/read flow using Jimmer and Redis.
- Create `src/main/kotlin/com/example/infrastructure/project/ProjectController.kt`: REST endpoints.
- Create `src/test/kotlin/com/example/infrastructure/InfrastructureApplicationTests.kt`: integration behavior test.

---

### Task 1: Build and runtime configuration

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `compose.yml`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/db/migration/V1__init.sql`

- [ ] **Step 1: Create Gradle settings**

Write `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "infrastructure"
```

- [ ] **Step 2: Create Gradle build**

Write `build.gradle.kts`:

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.springframework.boot") version "3.5.14"
    id("io.spring.dependency-management") version "1.1.7"
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.spring") version "2.2.21"
    id("com.google.devtools.ksp") version "2.2.21-2.0.5"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val jimmerVersion = "0.10.9"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.babyfish.jimmer:jimmer-spring-boot-starter:$jimmerVersion")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    ksp("org.babyfish.jimmer:jimmer-ksp:$jimmerVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

- [ ] **Step 3: Create local services**

Write `compose.yml`:

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: infrastructure
      POSTGRES_USER: infrastructure
      POSTGRES_PASSWORD: infrastructure
    ports:
      - "5432:5432"
    volumes:
      - postgres-data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U infrastructure -d infrastructure"]
      interval: 5s
      timeout: 5s
      retries: 10

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 5s
      timeout: 5s
      retries: 10

volumes:
  postgres-data:
```

- [ ] **Step 4: Create Spring configuration**

Write `src/main/resources/application.yml`:

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
  dialect: org.babyfish.jimmer.sql.dialect.PostgresDialect
  show-sql: true
  pretty-sql: true
```

- [ ] **Step 5: Create database migration**

Write `src/main/resources/db/migration/V1__init.sql`:

```sql
create table project (
    id uuid primary key,
    name varchar(200) not null,
    created_time timestamp not null
);
```

- [ ] **Step 6: Verify Gradle configuration phase**

Run on Windows:

```powershell
./gradlew.bat tasks
```

If no wrapper exists yet but local Gradle is installed, run:

```powershell
gradle tasks
```

Expected: Gradle lists available tasks without dependency resolution errors.

---

### Task 2: Application entry point and Jimmer entity

**Files:**
- Create: `src/test/kotlin/com/example/infrastructure/InfrastructureApplicationTests.kt`
- Create: `src/main/kotlin/com/example/infrastructure/InfrastructureApplication.kt`
- Create: `src/main/kotlin/com/example/infrastructure/project/Project.kt`

- [ ] **Step 1: Write failing context/entity generation test**

Write `src/test/kotlin/com/example/infrastructure/InfrastructureApplicationTests.kt`:

```kotlin
package com.example.infrastructure

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class InfrastructureApplicationTests {

    @Test
    fun `context loads`() {
    }
}
```

- [ ] **Step 2: Run test to verify it fails before application code exists**

Run:

```powershell
./gradlew.bat test --tests com.example.infrastructure.InfrastructureApplicationTests
```

Expected: FAIL because `@SpringBootConfiguration` / application configuration is missing.

- [ ] **Step 3: Add Spring Boot entry point**

Write `src/main/kotlin/com/example/infrastructure/InfrastructureApplication.kt`:

```kotlin
package com.example.infrastructure

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class InfrastructureApplication

fun main(args: Array<String>) {
    runApplication<InfrastructureApplication>(*args)
}
```

- [ ] **Step 4: Add Jimmer entity**

Write `src/main/kotlin/com/example/infrastructure/project/Project.kt`:

```kotlin
package com.example.infrastructure.project

import org.babyfish.jimmer.sql.Entity
import org.babyfish.jimmer.sql.Id
import org.babyfish.jimmer.sql.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "project")
interface Project {
    @Id
    val id: UUID
    val name: String
    val createdTime: LocalDateTime
}
```

- [ ] **Step 5: Run test to verify context passes when services are available**

Start dependencies:

```powershell
docker compose up -d
```

Run:

```powershell
./gradlew.bat test --tests com.example.infrastructure.InfrastructureApplicationTests
```

Expected: PASS and KSP generates Jimmer sources.

---

### Task 3: Project service, Redis probe, and HTTP API

**Files:**
- Modify: `src/test/kotlin/com/example/infrastructure/InfrastructureApplicationTests.kt`
- Create: `src/main/kotlin/com/example/infrastructure/project/ProjectDtos.kt`
- Create: `src/main/kotlin/com/example/infrastructure/cache/CacheProbeService.kt`
- Create: `src/main/kotlin/com/example/infrastructure/project/ProjectService.kt`
- Create: `src/main/kotlin/com/example/infrastructure/project/ProjectController.kt`

- [ ] **Step 1: Replace context-only test with behavior tests**

Write `src/test/kotlin/com/example/infrastructure/InfrastructureApplicationTests.kt`:

```kotlin
package com.example.infrastructure

import com.example.infrastructure.project.CreateProjectRequest
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
class InfrastructureApplicationTests(
    @Autowired private val mockMvc: MockMvc,
    @Autowired private val objectMapper: ObjectMapper,
) {

    @Test
    fun `creates and reads a project`() {
        val createResult = mockMvc.post("/api/projects") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(CreateProjectRequest(name = "core-platform"))
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { exists() }
            jsonPath("$.name") { value("core-platform") }
            jsonPath("$.createdTime") { exists() }
        }.andReturn()

        val id = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        mockMvc.get("/api/projects/$id")
            .andExpect {
                status { isOk() }
                jsonPath("$.id") { value(id) }
                jsonPath("$.name") { value("core-platform") }
            }
    }

    @Test
    fun `returns not found for missing project`() {
        val response = mockMvc.get("/api/projects/00000000-0000-0000-0000-000000000001")
            .andExpect {
                status { isNotFound() }
            }
            .andReturn()
            .response

        assertThat(response.contentAsString).isBlank()
    }
}
```

- [ ] **Step 2: Run behavior tests to verify they fail**

Run:

```powershell
./gradlew.bat test --tests com.example.infrastructure.InfrastructureApplicationTests
```

Expected: FAIL because request DTOs, controller, service, and API routes do not exist.

- [ ] **Step 3: Add DTOs**

Write `src/main/kotlin/com/example/infrastructure/project/ProjectDtos.kt`:

```kotlin
package com.example.infrastructure.project

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
    val createdTime: LocalDateTime,
)

fun Project.toResponse(): ProjectResponse = ProjectResponse(
    id = id,
    name = name,
    createdTime = createdTime,
)
```

- [ ] **Step 4: Add Redis cache probe service**

Write `src/main/kotlin/com/example/infrastructure/cache/CacheProbeService.kt`:

```kotlin
package com.example.infrastructure.cache

import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.util.UUID

@Service
class CacheProbeService(
    private val redisTemplate: StringRedisTemplate,
) {
    fun rememberProjectName(id: UUID, name: String) {
        redisTemplate.opsForValue().set(key(id), name, Duration.ofMinutes(10))
    }

    fun readProjectName(id: UUID): String? = redisTemplate.opsForValue().get(key(id))

    private fun key(id: UUID): String = "project:$id:name"
}
```

- [ ] **Step 5: Add Project service using Jimmer**

Write `src/main/kotlin/com/example/infrastructure/project/ProjectService.kt`:

```kotlin
package com.example.infrastructure.project

import com.example.infrastructure.cache.CacheProbeService
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class ProjectService(
    private val sqlClient: KSqlClient,
    private val cacheProbeService: CacheProbeService,
) {
    @Transactional
    fun create(request: CreateProjectRequest): ProjectResponse {
        val trimmedName = request.name.trim()
        require(trimmedName.isNotEmpty()) { "Project name must not be blank" }

        val entity = new(Project::class).by {
            id = UUID.randomUUID()
            name = trimmedName
            createdTime = LocalDateTime.now()
        }
        val saved = sqlClient.save(entity).modifiedEntity
        cacheProbeService.rememberProjectName(saved.id, saved.name)
        return saved.toResponse()
    }

    @Transactional(readOnly = true)
    fun findById(id: UUID): ProjectResponse? {
        cacheProbeService.readProjectName(id)
        return sqlClient.createQuery(Project::class) {
            where(table.id eq id)
            select(table)
        }.fetchOneOrNull()?.toResponse()
    }
}
```

- [ ] **Step 6: Add controller**

Write `src/main/kotlin/com/example/infrastructure/project/ProjectController.kt`:

```kotlin
package com.example.infrastructure.project

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/projects")
class ProjectController(
    private val projectService: ProjectService,
) {
    @PostMapping
    fun create(@Valid @RequestBody request: CreateProjectRequest): ProjectResponse = projectService.create(request)

    @GetMapping("/{id}")
    fun findById(@PathVariable id: UUID): ResponseEntity<ProjectResponse> =
        projectService.findById(id)?.let(ResponseEntity::ok) ?: ResponseEntity.notFound().build()
}
```

- [ ] **Step 7: Run behavior tests to verify pass**

Run:

```powershell
./gradlew.bat test --tests com.example.infrastructure.InfrastructureApplicationTests
```

Expected: PASS for create/read and missing-project behavior.

---

### Task 4: Wrapper and final verification

**Files:**
- Create if missing: `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`

- [ ] **Step 1: Generate Gradle wrapper if missing**

Run:

```powershell
gradle wrapper --gradle-version 8.14.3
```

Expected: wrapper files are created.

- [ ] **Step 2: Run full test suite through wrapper**

Run:

```powershell
./gradlew.bat test
```

Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Run application smoke test**

Start services:

```powershell
docker compose up -d
```

Start application:

```powershell
./gradlew.bat bootRun
```

Expected: application starts on port 8080 and logs show Flyway migration and Jimmer/Spring Boot startup without errors.

---

## Self-review

- Spec coverage: build, Spring Boot, Jimmer/KSP, PostgreSQL, Redis, Compose, Flyway, Project API, and tests are each mapped to tasks.
- Red-flag scan: no unresolved implementation markers or vague deferred-work instructions are present.
- Type consistency: package names, DTO names, endpoint paths, and `Project` fields are consistent across tasks.

package com.github.infrastructure.app.support

import org.springframework.test.context.DynamicPropertyRegistry
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Base class for integration tests that need a real PostgreSQL + Redis.
 *
 * The container lifecycle is tied to the JVM: containers are started once
 * per test suite and reused across tests (Ryuk handles teardown). Spring
 * components pick up the dynamic datasource and redis properties via
 * [registerProperties].
 *
 * Activate with either @Testcontainers on the subclass or by extending this
 * class. Spring tests should additionally carry @SpringBootTest.
 */
@Testcontainers
abstract class IntegrationTestContainerSupport {
    companion object {
        @JvmStatic
        protected val postgres: PostgreSQLContainer<*> = PostgreSQLContainer(DockerImageName.parse("postgres:16-alpine"))
            .withDatabaseName("infrastructure")
            .withUsername("infrastructure")
            .withPassword("infrastructure")
            .withReuse(true)
            .apply { start() }

        @JvmStatic
        protected val redis: GenericContainer<*> = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .withReuse(true)
            .apply { start() }

        @JvmStatic
        fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379).toString() }
        }
    }
}

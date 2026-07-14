package com.github.infrastructure.app.support

import com.github.infrastructure.app.InfrastructureApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

/**
 * Smoke test that proves the testcontainer setup actually starts Spring against a
 * real PostgreSQL and a real Redis. If Docker is unavailable the test class is
 * skipped via the {@code assumeDockerPresent} sentinel below.
 */
@SpringBootTest(classes = [InfrastructureApplication::class])
@ActiveProfiles("testcontainers")
class ContainerBackedApplicationContextTest : IntegrationTestContainerSupport() {
    companion object {
        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            registerProperties(registry)
        }
    }

    @Test
    fun `application context loads against postgres and redis`() {
        // Auto-configuration resolves the DataSource / Redis from the dynamic
        // properties; if any required bean is missing the @SpringBootTest will
        // already have failed at context startup.
    }
}

package com.github.infrastructure

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest(classes = [InfrastructureApplication::class])
@ActiveProfiles("test")
class InfrastructureApplicationTests {

    @Test
    fun `context loads`() {
    }
}

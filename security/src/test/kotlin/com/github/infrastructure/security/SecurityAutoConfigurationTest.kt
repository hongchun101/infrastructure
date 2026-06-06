package com.github.infrastructure.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.password.PasswordEncoder
import java.util.UUID

class SecurityAutoConfigurationTest {
    private val contextRunner = WebApplicationContextRunner()
        .withConfiguration(
            AutoConfigurations.of(
                JacksonAutoConfiguration::class.java,
                RedisAutoConfiguration::class.java,
                WebMvcAutoConfiguration::class.java,
                SecurityAutoConfiguration::class.java,
                com.github.infrastructure.security.SecurityAutoConfiguration::class.java,
            ),
        )
        .withUserConfiguration(TestAccountConfiguration::class.java)
        .withPropertyValues(
            "spring.data.redis.host=localhost",
            "spring.data.redis.port=6379",
        )

    @Test
    fun `security auto configuration creates auth beans`() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(SecurityProperties::class.java)
            assertThat(context).hasSingleBean(UuidTokenGenerator::class.java)
            assertThat(context).hasSingleBean(PasswordHasher::class.java)
            assertThat(context).hasSingleBean(AuthService::class.java)
            assertThat(context).hasSingleBean(AuthController::class.java)
            assertThat(context).hasSingleBean(AccessTokenAuthenticationFilter::class.java)
            assertThat(context).hasSingleBean(PermissionChecker::class.java)
            assertThat(context).hasSingleBean(PasswordEncoder::class.java)
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

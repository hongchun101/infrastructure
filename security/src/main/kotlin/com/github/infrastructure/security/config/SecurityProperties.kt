package com.github.infrastructure.security.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("infrastructure.security")
data class SecurityProperties(
    val accessTokenTtl: Duration = Duration.ofMinutes(30),
    val refreshTokenTtl: Duration = Duration.ofDays(7),
    val loginRateLimit: LoginRateLimit = LoginRateLimit(),
) {
    data class LoginRateLimit(
        val maxAttemptsPerPrincipal: Int = 10,
        val window: Duration = Duration.ofMinutes(5),
        val enabled: Boolean = true,
    )
}

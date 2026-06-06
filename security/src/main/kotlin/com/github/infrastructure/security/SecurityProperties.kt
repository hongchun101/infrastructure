package com.github.infrastructure.security

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("infrastructure.security")
data class SecurityProperties(
    val accessTokenTtl: Duration = Duration.ofMinutes(30),
    val refreshTokenTtl: Duration = Duration.ofDays(7),
)

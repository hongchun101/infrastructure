package com.github.infrastructure.alert.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties("infrastructure.alert")
data class AlertProperties(
    val enabled: Boolean = true,
    val email: EmailProperties = EmailProperties(),
    val autoResolve: AutoResolve = AutoResolve(),
) {
    data class EmailProperties(
        val from: String = "alerts@example.com",
        val subjectPrefix: String = "[Alert]",
    )

    data class AutoResolve(
        val enabled: Boolean = true,
        val idle: Duration = Duration.ofMinutes(15),
    )
}

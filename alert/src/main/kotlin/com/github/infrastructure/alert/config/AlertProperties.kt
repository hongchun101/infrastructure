package com.github.infrastructure.alert.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("infrastructure.alert")
data class AlertProperties(
    val enabled: Boolean = true,
    val email: EmailProperties = EmailProperties(),
) {
    data class EmailProperties(
        val from: String = "alerts@example.com",
        val subjectPrefix: String = "[Alert]",
    )
}

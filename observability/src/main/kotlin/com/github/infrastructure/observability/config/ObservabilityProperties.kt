package com.github.infrastructure.observability.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("infrastructure.observability")
data class ObservabilityProperties(
    val traceIdHeader: String = "X-Trace-Id",
    val mdcKey: String = "traceId",
)

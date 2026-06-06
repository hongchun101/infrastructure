package com.github.infrastructure.observability

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties::class)
class ObservabilityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun traceIdFilter(properties: ObservabilityProperties): TraceIdFilter = TraceIdFilter(properties)
}

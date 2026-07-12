package com.github.infrastructure.observability.config

import com.github.infrastructure.observability.filter.TraceIdFilter
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered

@AutoConfiguration
@EnableConfigurationProperties(ObservabilityProperties::class)
class ObservabilityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun traceIdFilter(properties: ObservabilityProperties): FilterRegistrationBean<TraceIdFilter> {
        val registration = FilterRegistrationBean(TraceIdFilter(properties))
        registration.order = Ordered.HIGHEST_PRECEDENCE
        return registration
    }
}

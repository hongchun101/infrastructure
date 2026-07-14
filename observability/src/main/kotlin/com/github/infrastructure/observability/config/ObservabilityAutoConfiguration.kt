package com.github.infrastructure.observability.config

import com.github.infrastructure.observability.filter.TraceIdFilter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer
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

    @Bean
    @ConditionalOnMissingBean
    fun commonTagsCustomizer(): MeterRegistryCustomizer<MeterRegistry> =
        MeterRegistryCustomizer { registry ->
            registry.config()
                .commonTags("application", "infrastructure")
                .meterFilter(MeterFilter.denyNameStartsWith("jvm.classes.loaded"))
        }
}

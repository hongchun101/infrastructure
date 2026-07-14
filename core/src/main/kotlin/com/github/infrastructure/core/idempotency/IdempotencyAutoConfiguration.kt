package com.github.infrastructure.core.idempotency

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.core.Ordered
import org.springframework.data.redis.core.StringRedisTemplate

@AutoConfiguration
@EnableConfigurationProperties(IdempotencyProperties::class)
@ConditionalOnProperty(prefix = "infrastructure.idempotency", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class IdempotencyAutoConfiguration {

    @Bean
    fun idempotencyStore(
        redis: StringRedisTemplate,
        objectMapper: ObjectMapper,
        properties: IdempotencyProperties,
    ): IdempotencyStore = RedisIdempotencyStore(redis, objectMapper, properties)

    @Bean
    fun idempotencyFilterRegistration(
        store: IdempotencyStore,
        objectMapper: ObjectMapper,
        properties: IdempotencyProperties,
    ): FilterRegistrationBean<IdempotencyFilter> {
        val filter = IdempotencyFilter(properties, store, objectMapper)
        val registration = FilterRegistrationBean(filter)
        // Run after Spring Security's filter chain so authentication is
        // established before we touch Redis, but before the dispatcher.
        registration.order = Ordered.HIGHEST_PRECEDENCE + 200
        registration.setName("idempotencyFilter")
        return registration
    }
}
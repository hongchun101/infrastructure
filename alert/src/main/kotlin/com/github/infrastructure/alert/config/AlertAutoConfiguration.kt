package com.github.infrastructure.alert.config

import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@AutoConfiguration
@EnableConfigurationProperties(AlertProperties::class)
@EnableAsync
@EnableScheduling
@ConditionalOnProperty(prefix = "infrastructure.alert", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class AlertAutoConfiguration

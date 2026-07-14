package com.github.infrastructure.scheduler.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.NestedConfigurationProperty

@ConfigurationProperties(prefix = "infrastructure.scheduler")
class SchedulerProperties {

    var enabled: Boolean = true

    var scanIntervalMs: Long = 1000

    var batchSize: Int = 20

    var workerThreads: Int = 4

    @NestedConfigurationProperty
    var defaults: Defaults = Defaults()

    class Defaults {
        var retryMaxAttempts: Int = 3
        var retryInitialBackoffSeconds: Long = 5
        var retryMaxBackoffSeconds: Long = 300
        var retryMultiplier: Double = 2.0
        var timeoutSeconds: Int = 300
    }
}
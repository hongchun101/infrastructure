package com.github.infrastructure.export.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "infrastructure.export")
class ExportProperties {

    var enabled: Boolean = true

    var runnerIntervalSeconds: Int = 5

    var pageSize: Int = 5000

    var batchSize: Int = 5

    var maxRowsPerJob: Long = 1_000_000

    var defaultTtlDays: Int = 7

    var maxConcurrentPerUser: Int = 3
}
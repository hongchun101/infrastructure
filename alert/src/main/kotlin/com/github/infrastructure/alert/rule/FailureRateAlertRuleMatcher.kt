package com.github.infrastructure.alert.rule

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.alert.entity.AlertRule
import com.github.infrastructure.alert.entity.AlertRuleType
import com.github.infrastructure.alert.repository.AlertWindowCounterRepository
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDateTime

@Component
class FailureRateAlertRuleMatcher(
    private val windowCounterRepository: AlertWindowCounterRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) : AlertRuleMatcher {
    override val type = AlertRuleType.FAILURE_RATE

    override fun matches(rule: AlertRule, signal: OperationLogSignal): AlertMatch? {
        if (!rule.sourceModule.isNullOrBlank() && rule.sourceModule != signal.module) return null
        if (!rule.sourceAction.isNullOrBlank() && rule.sourceAction != signal.action) return null
        val config = parseConfig(rule.config)
        val now = LocalDateTime.now(clock)
        val bucket = now.withSecond(0).withNano(0)
        val windowStart = bucket.minusMinutes(config.windowMinutes.toLong())
        val summary = windowCounterRepository.summarize(rule.id, windowStart, bucket)
        if (summary.total < config.minSamples) return null
        if (summary.rate < config.minFailureRate) return null
        return AlertMatch(
            summary = "Failure rate ${"%.0f".format(summary.rate * 100)}% over last " +
                "${config.windowMinutes}m for ${signal.module}/${signal.action} " +
                "(${summary.failed}/${summary.total})",
            fingerprintSalt = "fr:$bucket",
            extraDetail = objectMapper.createObjectNode()
                .put("windowMinutes", config.windowMinutes)
                .put("total", summary.total)
                .put("failed", summary.failed)
                .put("rate", summary.rate),
        )
    }

    private data class FailureRateConfig(
        val windowMinutes: Int,
        val minFailureRate: Double,
        val minSamples: Long,
    )

    private fun parseConfig(json: String): FailureRateConfig = runCatching {
        val node = objectMapper.readTree(json)
        FailureRateConfig(
            windowMinutes = node.path("windowMinutes").asInt(5).coerceIn(1, 240),
            minFailureRate = node.path("minFailureRate").asDouble(0.5).coerceIn(0.0, 1.0),
            minSamples = node.path("minSamples").asLong(10).coerceAtLeast(1L),
        )
    }.getOrElse { FailureRateConfig(5, 0.5, 10) }
}

package com.github.infrastructure.alert.rule

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.alert.entity.AlertRule
import com.github.infrastructure.alert.entity.AlertRuleType
import org.springframework.stereotype.Component

@Component
class StatusCodeAlertRuleMatcher(
    private val objectMapper: ObjectMapper,
) : AlertRuleMatcher {
    override val type = AlertRuleType.STATUS_CODE

    override fun matches(rule: AlertRule, signal: OperationLogSignal): AlertMatch? {
        val status = signal.responseStatus
        if (status < 400) return null
        if (!rule.sourceModule.isNullOrBlank() && rule.sourceModule != signal.module) return null
        if (!rule.sourceAction.isNullOrBlank() && rule.sourceAction != signal.action) return null
        val config = parseConfig(rule.config)
        val minStatus = config.minStatus
        if (status < minStatus) return null
        val bucket = "${status / 100}xx"
        return AlertMatch(
            summary = "HTTP $status on ${signal.module}/${signal.action} (${signal.method} ${signal.path})",
            fingerprintSalt = "status:$bucket",
            extraDetail = objectMapper.createObjectNode().put("status", status),
        )
    }

    private data class StatusConfig(val minStatus: Int)

    private fun parseConfig(json: String): StatusConfig = runCatching {
        val node = objectMapper.readTree(json)
        StatusConfig(node.path("minStatus").asInt(500))
    }.getOrElse { StatusConfig(500) }
}

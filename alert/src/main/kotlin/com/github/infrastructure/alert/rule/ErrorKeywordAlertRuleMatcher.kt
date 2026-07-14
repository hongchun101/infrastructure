package com.github.infrastructure.alert.rule

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.alert.entity.AlertRule
import com.github.infrastructure.alert.entity.AlertRuleType
import org.springframework.stereotype.Component

@Component
class ErrorKeywordAlertRuleMatcher(
    private val objectMapper: ObjectMapper,
) : AlertRuleMatcher {
    override val type = AlertRuleType.ERROR_KEYWORD

    override fun matches(rule: AlertRule, signal: OperationLogSignal): AlertMatch? {
        if (signal.success) return null
        val errorMessage = signal.errorMessage ?: return null
        if (!rule.sourceModule.isNullOrBlank() && rule.sourceModule != signal.module) return null
        if (!rule.sourceAction.isNullOrBlank() && rule.sourceAction != signal.action) return null
        val config = parseConfig(rule.config)
        val keywords = config.keywords
        if (keywords.isEmpty()) return null
        val matched = keywords.firstOrNull { keyword ->
            errorMessage.contains(keyword, ignoreCase = config.caseInsensitive)
        } ?: return null
        val summary = "Matched keyword '$matched' on ${signal.module}/${signal.action}: " +
            errorMessage.take(200)
        return AlertMatch(
            summary = summary,
            fingerprintSalt = "kw:$matched",
            extraDetail = objectMapper.createObjectNode().put("matchedKeyword", matched),
        )
    }

    private data class KeywordConfig(
        val keywords: List<String>,
        val caseInsensitive: Boolean,
    )

    private fun parseConfig(json: String): KeywordConfig = runCatching {
        val node = objectMapper.readTree(json)
        val keywords = if (node.has("keywords") && node.get("keywords").isArray) {
            node.get("keywords").mapNotNull { it.asText().takeIf(String::isNotBlank) }
        } else {
            emptyList()
        }
        val caseInsensitive = node.path("caseInsensitive").asBoolean(true)
        KeywordConfig(keywords, caseInsensitive)
    }.getOrElse { KeywordConfig(emptyList(), true) }
}

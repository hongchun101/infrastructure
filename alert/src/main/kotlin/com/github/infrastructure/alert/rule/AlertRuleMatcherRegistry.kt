package com.github.infrastructure.alert.rule

import com.github.infrastructure.alert.entity.AlertRuleType
import org.springframework.stereotype.Component

@Component
class AlertRuleMatcherRegistry(
    matchers: List<AlertRuleMatcher>,
) {
    private val byType: Map<AlertRuleType, AlertRuleMatcher> =
        matchers.associateBy { it.type }

    fun get(type: AlertRuleType): AlertRuleMatcher? = byType[type]

    fun types(): Set<AlertRuleType> = byType.keys
}

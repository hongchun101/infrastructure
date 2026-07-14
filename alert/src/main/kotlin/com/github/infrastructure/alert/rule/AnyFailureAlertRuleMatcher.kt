package com.github.infrastructure.alert.rule

import com.github.infrastructure.alert.entity.AlertRule
import com.github.infrastructure.alert.entity.AlertRuleType
import org.springframework.stereotype.Component

@Component
class AnyFailureAlertRuleMatcher : AlertRuleMatcher {
    override val type = AlertRuleType.ANY_FAILURE

    override fun matches(rule: AlertRule, signal: OperationLogSignal): AlertMatch? {
        if (signal.success) return null
        if (!rule.sourceModule.isNullOrBlank() && rule.sourceModule != signal.module) return null
        if (!rule.sourceAction.isNullOrBlank() && rule.sourceAction != signal.action) return null
        return AlertMatch(
            summary = "Operation failed: ${signal.module}/${signal.action} (${signal.method} ${signal.path})",
            fingerprintSalt = "any-failure",
        )
    }
}

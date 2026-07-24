package com.github.infrastructure.alert.rule

import com.fasterxml.jackson.databind.JsonNode
import com.github.infrastructure.alert.entity.AlertRule
import com.github.infrastructure.alert.entity.AlertRuleType

/**
 * Snapshot of a rule hit produced by a matcher. The same fingerprint is used as
 * the primary dedup key on [com.github.infrastructure.alert.entity.AlertEvent].
 */
data class AlertMatch(
    val summary: String,
    val fingerprintSalt: String,
    val extraDetail: JsonNode? = null,
)

interface AlertRuleMatcher {
    val type: AlertRuleType

    /**
     * Decide whether this rule matches the given operation log signal.
     *
     * Implementations should be cheap; they are invoked on every received
     * signal for every enabled rule of the supported type.
     */
    fun matches(rule: AlertRule, signal: OperationLogSignal): AlertMatch?
}

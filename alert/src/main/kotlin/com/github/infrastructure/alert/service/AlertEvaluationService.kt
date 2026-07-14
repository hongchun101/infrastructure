package com.github.infrastructure.alert.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.infrastructure.alert.entity.AlertEvent
import com.github.infrastructure.alert.entity.AlertRule
import com.github.infrastructure.alert.entity.AlertRuleType
import com.github.infrastructure.alert.entity.AlertWindowCounter
import com.github.infrastructure.alert.repository.AlertEventRepository
import com.github.infrastructure.alert.repository.AlertRuleRepository
import com.github.infrastructure.alert.repository.AlertWindowCounterRepository
import com.github.infrastructure.alert.rule.AlertRuleMatcherRegistry
import com.github.infrastructure.alert.rule.OperationLogSignal
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlertEvaluationService(
    private val ruleRepository: AlertRuleRepository,
    private val eventRepository: AlertEventRepository,
    private val windowCounterRepository: AlertWindowCounterRepository,
    private val matcherRegistry: AlertRuleMatcherRegistry,
    private val dispatchService: AlertDispatchService,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun evaluate(signal: OperationLogSignal) {
        val rules = ruleRepository.findAllEnabled()
        if (rules.isEmpty()) return
        incrementFailureRateCounters(rules, signal)
        for (rule in rules) {
            try {
                evaluateOne(rule, signal)
            } catch (e: Exception) {
                log.error(
                    "alert evaluation failed for rule {} module {} action {}",
                    rule.code, signal.module, signal.action, e,
                )
            }
        }
    }

    private fun incrementFailureRateCounters(rules: List<AlertRule>, signal: OperationLogSignal) {
        val now = LocalDateTime.now(clock)
        val bucket = now.withSecond(0).withNano(0)
        rules.forEach { rule ->
            if (runCatching { AlertRuleType.valueOf(rule.ruleType) }.getOrNull() != AlertRuleType.FAILURE_RATE) {
                return@forEach
            }
            if (!rule.sourceModule.isNullOrBlank() && rule.sourceModule != signal.module) return@forEach
            if (!rule.sourceAction.isNullOrBlank() && rule.sourceAction != signal.action) return@forEach
            val existing = windowCounterRepository.findByRuleAndBucket(rule.id, bucket)
            if (existing == null) {
                windowCounterRepository.save(
                    AlertWindowCounter {
                        id = UUID.randomUUID()
                        this.ruleId = rule.id
                        this.bucketMinute = bucket
                        this.totalCount = 1
                        this.failedCount = if (signal.success) 0 else 1
                        this.createdTime = now
                        this.updatedTime = now
                    },
                )
            } else {
                windowCounterRepository.save(
                    AlertWindowCounter {
                        id = existing.id
                        this.ruleId = existing.ruleId
                        this.bucketMinute = existing.bucketMinute
                        this.totalCount = existing.totalCount + 1
                        this.failedCount = existing.failedCount + if (signal.success) 0 else 1
                        this.createdTime = existing.createdTime
                        this.updatedTime = now
                    },
                )
            }
        }
    }

    private fun evaluateOne(rule: AlertRule, signal: OperationLogSignal) {
        val type = runCatching { AlertRuleType.valueOf(rule.ruleType) }.getOrNull() ?: return
        val matcher = matcherRegistry.get(type) ?: return
        val match = matcher.matches(rule, signal) ?: return
        val fingerprint = AlertDispatchService.fingerprint(
            ruleId = rule.id,
            sourceModule = signal.module,
            sourceAction = signal.action,
            salt = match.fingerprintSalt,
        )
        val now = LocalDateTime.now(clock)
        val existing = eventRepository.findByFingerprint(fingerprint)
        if (existing != null) {
            val wasResolved = existing.resolved
            val occurrences = existing.occurrences + 1
            eventRepository.save(
                AlertEvent {
                    id = existing.id
                    ruleId = existing.ruleId
                    this.fingerprint = fingerprint
                    sourceModule = existing.sourceModule ?: signal.module
                    sourceAction = existing.sourceAction ?: signal.action
                    severity = existing.severity
                    summary = match.summary
                    detail = mergeDetail(existing.detail, match.extraDetail)
                    firstSeenAt = existing.firstSeenAt
                    lastSeenAt = now
                    this.occurrences = occurrences
                    resolved = if (wasResolved) false else existing.resolved
                    resolvedAt = if (wasResolved) null else existing.resolvedAt
                    createdTime = existing.createdTime
                    updatedTime = now
                },
            )
            if (wasResolved) {
                dispatchService.dispatch(rule, existing)
            }
            return
        }
        val detailJson = match.extraDetail?.let { objectMapper.writeValueAsString(it) }
        val saved = eventRepository.save(
            AlertEvent {
                id = UUID.randomUUID()
                ruleId = rule.id
                this.fingerprint = fingerprint
                sourceModule = signal.module
                sourceAction = signal.action
                severity = rule.severity
                summary = match.summary
                detail = detailJson
                firstSeenAt = now
                lastSeenAt = now
                occurrences = 1
                resolved = false
                resolvedAt = null
                createdTime = now
                updatedTime = now
            },
        ).modifiedEntity
        dispatchService.dispatch(rule, saved)
    }

    private fun mergeDetail(existingJson: String?, extra: JsonNode?): String? {
        if (existingJson.isNullOrBlank() && extra == null) return null
        return try {
            val merged: ObjectNode = if (existingJson.isNullOrBlank()) {
                objectMapper.createObjectNode()
            } else {
                val parsed = objectMapper.readTree(existingJson)
                (parsed as? ObjectNode) ?: objectMapper.createObjectNode()
            }
            (extra as? ObjectNode)?.fields()?.forEach { (k, v) ->
                merged.set<JsonNode>(k, v)
            }
            objectMapper.writeValueAsString(merged)
        } catch (e: Exception) {
            log.debug("failed to merge alert event detail, keeping existing", e)
            existingJson
        }
    }
}

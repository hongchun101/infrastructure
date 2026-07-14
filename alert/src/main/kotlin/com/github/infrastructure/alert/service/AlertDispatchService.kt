package com.github.infrastructure.alert.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.infrastructure.alert.channel.AlertChannel
import com.github.infrastructure.alert.channel.AlertChannelRegistry
import com.github.infrastructure.alert.channel.ChannelSendOutcome
import com.github.infrastructure.alert.dto.AlertRuleChannelSpec
import com.github.infrastructure.alert.entity.AlertChannelType
import com.github.infrastructure.alert.entity.AlertEvent
import com.github.infrastructure.alert.entity.AlertNotification
import com.github.infrastructure.alert.entity.AlertNotificationStatus
import com.github.infrastructure.alert.entity.AlertRule
import com.github.infrastructure.alert.entity.Severity
import com.github.infrastructure.alert.repository.AlertEventRepository
import com.github.infrastructure.alert.repository.AlertNotificationRepository
import java.security.MessageDigest
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlertDispatchService(
    private val eventRepository: AlertEventRepository,
    private val notificationRepository: AlertNotificationRepository,
    private val channelRegistry: AlertChannelRegistry,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun dispatch(rule: AlertRule, event: AlertEvent) {
        val channels = decodeChannels(rule)
        if (channels.isEmpty()) {
            log.debug("rule {} has no channels, skipping dispatch", rule.code)
            return
        }
        val freshEvent = eventRepository.findById(event.id) ?: event
        channels.forEach { spec ->
            val channel: AlertChannel? = channelRegistry.get(spec.type)
            if (channel == null) {
                log.warn("no implementation for channel {}", spec.type)
                saveNotification(freshEvent, spec, ChannelSendOutcome(null, "no implementation registered"))
                return@forEach
            }
            val payload = buildPayload(rule, freshEvent)
            val outcome = try {
                channel.send(spec.target, payload)
            } catch (e: Exception) {
                ChannelSendOutcome(httpStatus = null, errorMessage = e.message?.take(2000))
            }
            saveNotification(freshEvent, spec, outcome)
        }
    }

    private fun saveNotification(
        event: AlertEvent,
        spec: AlertRuleChannelSpec,
        outcome: ChannelSendOutcome,
    ) {
        val now = LocalDateTime.now(clock)
        val success = outcome.httpStatus != null && outcome.httpStatus in 200..299
        val status = if (success) AlertNotificationStatus.SUCCESS else AlertNotificationStatus.FAILED
        notificationRepository.save(
            AlertNotification {
                id = UUID.randomUUID()
                this.eventId = event.id
                channel = spec.type.name
                target = spec.target
                this.status = status.name
                httpStatus = outcome.httpStatus
                errorMessage = outcome.errorMessage
                payload = null
                sentAt = now
                createdTime = now
            },
        )
        if (!success) {
            log.warn(
                "alert notification failed for event {} via {} target {}: {}",
                event.id, spec.type, spec.target, outcome.errorMessage,
            )
        }
    }

    private fun decodeChannels(rule: AlertRule): List<AlertRuleChannelSpec> = runCatching {
        val node = objectMapper.readTree(rule.channels)
        if (!node.isArray) return@runCatching emptyList()
        node.map {
            AlertRuleChannelSpec(
                type = AlertChannelType.valueOf(it.get("type").asText()),
                target = it.get("target").asText(),
            )
        }
    }.getOrElse { emptyList() }

    private fun buildPayload(rule: AlertRule, event: AlertEvent): ObjectNode {
        val detailJson = event.detail?.takeIf { it.isNotBlank() }?.let { objectMapper.readTree(it) }
        val node = objectMapper.createObjectNode()
        node.put("ruleCode", rule.code)
        node.put("ruleName", rule.name)
        node.put("severity", Severity.valueOf(event.severity).name)
        node.put("summary", event.summary)
        node.put("sourceModule", event.sourceModule ?: "")
        node.put("sourceAction", event.sourceAction ?: "")
        node.put("firstSeenAt", event.firstSeenAt.toString())
        node.put("lastSeenAt", event.lastSeenAt.toString())
        node.put("occurrences", event.occurrences)
        node.put("resolved", event.resolved)
        if (detailJson != null) node.set<com.fasterxml.jackson.databind.JsonNode>("detail", detailJson)
        return node
    }

    companion object {
        fun fingerprint(
            ruleId: UUID,
            sourceModule: String?,
            sourceAction: String?,
            salt: String,
        ): String {
            val raw = "${ruleId}|${sourceModule ?: ""}|${sourceAction ?: ""}|${salt}"
            val digest = MessageDigest.getInstance("SHA-256").digest(raw.toByteArray())
            return digest.joinToString("") { "%02x".format(it) }.take(64)
        }
    }
}

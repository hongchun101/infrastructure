package com.github.infrastructure.alert.channel

import com.fasterxml.jackson.databind.JsonNode
import com.github.infrastructure.alert.config.AlertProperties
import com.github.infrastructure.alert.entity.AlertChannelType
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.mail.MailException
import org.springframework.mail.SimpleMailMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component

@Component
@ConditionalOnBean(JavaMailSender::class)
class EmailAlertChannel(
    private val mailSender: JavaMailSender,
    private val alertProperties: AlertProperties,
) : AlertChannel {
    override val type = AlertChannelType.EMAIL

    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(target: String, payload: JsonNode): ChannelSendOutcome {
        val subject = buildSubject(payload)
        val body = buildBody(payload)
        return try {
            val message = SimpleMailMessage().apply {
                from = alertProperties.email.from
                setTo(target)
                this.subject = subject
                this.text = body
            }
            mailSender.send(message)
            ChannelSendOutcome(httpStatus = 250, errorMessage = null)
        } catch (e: MailException) {
            log.warn("failed to deliver email alert to {}: {}", target, e.message)
            ChannelSendOutcome(httpStatus = null, errorMessage = e.message?.take(2000))
        } catch (e: Exception) {
            log.warn("unexpected error delivering email alert to {}: {}", target, e.message)
            ChannelSendOutcome(httpStatus = null, errorMessage = e.message?.take(2000))
        }
    }

    private fun buildSubject(payload: JsonNode): String {
        val severity = payload.path("severity").asText("INFO")
        val ruleName = payload.path("ruleName").asText("alert")
        return "${alertProperties.email.subjectPrefix} $severity — $ruleName"
    }

    private fun buildBody(payload: JsonNode): String = buildString {
        appendLine("Severity:   ${payload.path("severity").asText()}")
        appendLine("Rule:       ${payload.path("ruleCode").asText()} (${payload.path("ruleName").asText()})")
        appendLine("Summary:    ${payload.path("summary").asText()}")
        val module = payload.path("sourceModule").asText()
        val action = payload.path("sourceAction").asText()
        if (module.isNotBlank() || action.isNotBlank()) {
            appendLine("Source:     $module / $action")
        }
        appendLine("First seen: ${payload.path("firstSeenAt").asText()}")
        appendLine("Last seen:  ${payload.path("lastSeenAt").asText()}")
        appendLine("Occurrences: ${payload.path("occurrences").asLong()}")
        val detail = payload.path("detail")
        if (!detail.isMissingNode && detail.isObject && detail.size() > 0) {
            appendLine()
            appendLine("Detail:")
            detail.fields().forEach { (k, v) -> appendLine("  $k: $v") }
        }
    }
}

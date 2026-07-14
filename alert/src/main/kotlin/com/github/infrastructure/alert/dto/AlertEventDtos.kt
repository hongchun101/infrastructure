package com.github.infrastructure.alert.dto

import com.fasterxml.jackson.databind.JsonNode
import com.github.infrastructure.alert.entity.Severity
import java.time.LocalDateTime
import java.util.UUID

data class AlertEventResponse(
    val id: UUID,
    val ruleId: UUID,
    val ruleCode: String?,
    val ruleName: String?,
    val fingerprint: String,
    val sourceModule: String?,
    val sourceAction: String?,
    val severity: Severity,
    val summary: String,
    val detail: JsonNode?,
    val firstSeenAt: LocalDateTime,
    val lastSeenAt: LocalDateTime,
    val occurrences: Long,
    val resolved: Boolean,
    val resolvedAt: LocalDateTime?,
    val createdTime: LocalDateTime,
    val updatedTime: LocalDateTime,
)

package com.github.infrastructure.alert.dto

import com.fasterxml.jackson.databind.JsonNode
import com.github.infrastructure.alert.entity.AlertNotificationStatus
import java.time.LocalDateTime
import java.util.UUID

data class AlertNotificationResponse(
    val id: UUID,
    val eventId: UUID,
    val channel: String,
    val target: String,
    val status: AlertNotificationStatus,
    val httpStatus: Int?,
    val errorMessage: String?,
    val payload: JsonNode?,
    val sentAt: LocalDateTime,
    val createdTime: LocalDateTime,
)

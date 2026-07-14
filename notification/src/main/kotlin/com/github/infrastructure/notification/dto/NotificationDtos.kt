package com.github.infrastructure.notification.dto

import com.github.infrastructure.notification.entity.UserNotification
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class SendNotificationRequest(
    @field:NotBlank
    @field:Size(max = 255)
    val title: String,

    @field:NotBlank
    @field:Size(max = 5000)
    val content: String,

    val category: String = "system",

    val priority: Int = 0,

    @field:Size(max = 500)
    val linkUrl: String? = null,

    val payload: String? = null,

    @field:Min(1) @field:Max(365)
    val ttlDays: Int? = null,
)

data class SendBatchNotificationRequest(
    @field:Size(min = 1, max = 1000)
    val recipients: List<UUID>,

    @field:NotBlank
    @field:Size(max = 255)
    val title: String,

    @field:NotBlank
    @field:Size(max = 5000)
    val content: String,

    val category: String = "system",

    val priority: Int = 0,

    @field:Size(max = 500)
    val linkUrl: String? = null,

    val payload: String? = null,

    @field:Min(1) @field:Max(365)
    val ttlDays: Int? = null,
)

data class NotificationResponse(
    val id: Long,
    val recipientUserId: UUID,
    val category: String,
    val title: String,
    val content: String,
    val linkUrl: String?,
    val priority: Int,
    val read: Boolean,
    val readAt: LocalDateTime?,
    val archivedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val expiresAt: LocalDateTime?,
) {
    companion object {
        fun from(entity: UserNotification): NotificationResponse = NotificationResponse(
            id = entity.id,
            recipientUserId = entity.recipientUserId,
            category = entity.category,
            title = entity.title,
            content = entity.content,
            linkUrl = entity.linkUrl,
            priority = entity.priority,
            read = entity.readAt != null,
            readAt = entity.readAt,
            archivedAt = entity.archivedAt,
            createdAt = entity.createdAt,
            expiresAt = entity.expiresAt,
        )
    }
}

data class UnreadCountResponse(val count: Long)

data class BatchSendResponse(val created: Int)
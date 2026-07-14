package com.github.infrastructure.notification.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.infrastructure.notification.dto.BatchSendResponse
import com.github.infrastructure.notification.dto.NotificationResponse
import com.github.infrastructure.notification.dto.SendBatchNotificationRequest
import com.github.infrastructure.notification.dto.SendNotificationRequest
import com.github.infrastructure.notification.dto.UnreadCountResponse
import com.github.infrastructure.notification.entity.UserNotification
import com.github.infrastructure.notification.repository.UserNotificationRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class UserNotificationService(
    private val repository: UserNotificationRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun sendToOne(recipient: UUID, request: SendNotificationRequest): NotificationResponse {
        val batch = SendBatchNotificationRequest(
            recipients = listOf(recipient),
            title = request.title,
            content = request.content,
            category = request.category,
            priority = request.priority,
            linkUrl = request.linkUrl,
            payload = request.payload,
            ttlDays = request.ttlDays,
        )
        val entity = persist(listOf(recipient), batch).single()
        return NotificationResponse.from(entity)
    }

    @Transactional
    fun sendToMany(request: SendBatchNotificationRequest): BatchSendResponse {
        val recipients = request.recipients.distinct()
        if (recipients.isEmpty()) {
            throw IllegalArgumentException("recipients must not be empty")
        }
        val entities = persist(recipients, request)
        return BatchSendResponse(created = entities.size)
    }

    fun listInbox(
        recipient: UUID,
        onlyUnread: Boolean,
        includeArchived: Boolean,
        limit: Int,
    ): List<NotificationResponse> = repository.listInbox(recipient, onlyUnread, includeArchived, limit)
        .map(NotificationResponse::from)

    fun unreadCount(recipient: UUID): UnreadCountResponse = UnreadCountResponse(repository.countUnread(recipient))

    @Transactional
    fun markRead(id: Long, recipient: UUID): NotificationResponse {
        val existing = repository.findByIdAndRecipient(id, recipient)
            ?: throw NoSuchElementException("notification $id not found")
        if (existing.readAt != null) return NotificationResponse.from(existing)
        val now = LocalDateTime.now(clock)
        val updated = repository.save(
            UserNotification {
                this.id = existing.id
                recipientUserId = existing.recipientUserId
                category = existing.category
                title = existing.title
                content = existing.content
                linkUrl = existing.linkUrl
                payload = existing.payload
                priority = existing.priority
                readAt = now
                archivedAt = existing.archivedAt
                createdAt = existing.createdAt
                expiresAt = existing.expiresAt
            },
        ).modifiedEntity
        return NotificationResponse.from(updated)
    }

    @Transactional
    fun markAllRead(recipient: UUID): Int {
        val now = LocalDateTime.now(clock)
        val pending = repository.listInbox(recipient, onlyUnread = true, includeArchived = false, limit = 200)
        pending.forEach { row ->
            repository.save(
                UserNotification {
                    this.id = row.id
                    recipientUserId = row.recipientUserId
                    category = row.category
                    title = row.title
                    content = row.content
                    linkUrl = row.linkUrl
                    payload = row.payload
                    priority = row.priority
                    readAt = now
                    archivedAt = row.archivedAt
                    createdAt = row.createdAt
                    expiresAt = row.expiresAt
                },
            )
        }
        return pending.size
    }

    @Transactional
    fun archive(id: Long, recipient: UUID): NotificationResponse {
        val existing = repository.findByIdAndRecipient(id, recipient)
            ?: throw NoSuchElementException("notification $id not found")
        if (existing.archivedAt != null) return NotificationResponse.from(existing)
        val now = LocalDateTime.now(clock)
        val updated = repository.save(
            UserNotification {
                this.id = existing.id
                recipientUserId = existing.recipientUserId
                category = existing.category
                title = existing.title
                content = existing.content
                linkUrl = existing.linkUrl
                payload = existing.payload
                priority = existing.priority
                readAt = existing.readAt
                archivedAt = now
                createdAt = existing.createdAt
                expiresAt = existing.expiresAt
            },
        ).modifiedEntity
        return NotificationResponse.from(updated)
    }

    private fun persist(
        recipients: List<UUID>,
        request: SendBatchNotificationRequest,
    ): List<UserNotification> {
        val now = LocalDateTime.now(clock)
        val expiresAt = request.ttlDays?.let { now.plusDays(it.toLong()) }
        val validatedPayload = request.payload?.takeIf { it.isNotBlank() }?.let { validateJson(it) }
        return recipients.map { recipient ->
            repository.save(
                UserNotification {
                    recipientUserId = recipient
                    category = request.category
                    title = request.title
                    content = request.content
                    linkUrl = request.linkUrl
                    payload = validatedPayload
                    priority = request.priority.coerceIn(0, 2)
                    readAt = null
                    archivedAt = null
                    createdAt = now
                    this.expiresAt = expiresAt
                },
            ).modifiedEntity
        }
    }

    private fun validateJson(payload: String): String = try {
        val tree = objectMapper.readTree(payload)
        require(tree is ObjectNode) { "payload must be a JSON object" }
        payload
    } catch (e: Exception) {
        log.warn("dropping invalid notification payload: {}", e.message)
        throw IllegalArgumentException("payload must be a JSON object", e)
    }
}
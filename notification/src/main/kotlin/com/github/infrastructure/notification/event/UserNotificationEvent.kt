package com.github.infrastructure.notification.event

import java.util.UUID

/**
 * Spring application event published by business modules whenever a user should
 * be notified. The notification module's [UserNotificationListener] consumes it
 * and persists one [com.github.infrastructure.notification.entity.UserNotification]
 * per recipient.
 *
 * Keep this data class flat and free of domain entities so that other modules do
 * not need to depend on Jimmer or the notification module itself.
 */
data class UserNotificationEvent(
    val recipients: List<UUID>,
    val title: String,
    val content: String,
    val category: String = "system",
    val priority: Int = 0,
    val linkUrl: String? = null,
    val payload: String? = null,
    val ttlDays: Int? = null,
    val actor: String? = null,
) {
    init {
        require(recipients.isNotEmpty()) { "recipients must not be empty" }
        require(title.isNotBlank()) { "title must not be blank" }
        require(content.isNotBlank()) { "content must not be blank" }
        require(priority in 0..2) { "priority must be 0..2" }
    }
}
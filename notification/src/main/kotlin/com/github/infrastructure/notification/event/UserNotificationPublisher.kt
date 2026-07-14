package com.github.infrastructure.notification.event

import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component

/**
 * Convenience publisher for business modules. Inject this to broadcast a
 * [UserNotificationEvent] without needing to depend on Spring's
 * ApplicationEventPublisher directly.
 */
@Component
class UserNotificationPublisher(
    private val applicationEventPublisher: ApplicationEventPublisher,
) {
    fun publish(event: UserNotificationEvent) {
        applicationEventPublisher.publishEvent(event)
    }

    fun publish(
        recipients: List<java.util.UUID>,
        title: String,
        content: String,
        category: String = "system",
        priority: Int = 0,
        linkUrl: String? = null,
        payload: String? = null,
        ttlDays: Int? = null,
        actor: String? = null,
    ) {
        publish(
            UserNotificationEvent(
                recipients = recipients,
                title = title,
                content = content,
                category = category,
                priority = priority,
                linkUrl = linkUrl,
                payload = payload,
                ttlDays = ttlDays,
                actor = actor,
            ),
        )
    }
}
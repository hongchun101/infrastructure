package com.github.infrastructure.notification.event

import com.github.infrastructure.notification.dto.SendBatchNotificationRequest
import com.github.infrastructure.notification.service.UserNotificationService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class UserNotificationListener(
    private val service: UserNotificationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun on(event: UserNotificationEvent) {
        try {
            service.sendToMany(
                SendBatchNotificationRequest(
                    recipients = event.recipients,
                    title = event.title,
                    content = event.content,
                    category = event.category,
                    priority = event.priority,
                    linkUrl = event.linkUrl,
                    payload = event.payload,
                    ttlDays = event.ttlDays,
                ),
            )
        } catch (e: Exception) {
            log.warn(
                "failed to deliver user notification '{}' to {} recipients: {}",
                event.title, event.recipients.size, e.message,
            )
        }
    }
}
package com.github.infrastructure.app.announcement.event

import com.github.infrastructure.app.user.repository.BackendAccountRepository
import com.github.infrastructure.notification.event.UserNotificationPublisher
import org.babyfish.jimmer.sql.kt.ast.expression.`eq?`
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * Bridges announcement publication events to user notifications. When an
 * announcement is published we fan out a notification to every active
 * backend account so administrators see the new content in their inbox.
 *
 * To avoid pulling the full announcement body we deliberately cap the
 * notification content to a summary; the full text remains reachable via
 * the announcement UI.
 */
@Component
class AnnouncementNotificationBridge(
    private val backendAccountRepository: BackendAccountRepository,
    private val notificationPublisher: UserNotificationPublisher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener
    fun on(event: AnnouncementPublishedEvent) {
        val active = backendAccountRepository.findActiveAccountIds()
        if (active.isEmpty()) {
            log.debug("no active backend accounts; skipping notification fan-out")
            return
        }
        notificationPublisher.publish(
            recipients = active,
            title = "新公告: ${event.title}",
            content = event.summary ?: event.content.take(120),
            category = "announcement",
            priority = if (event.priority >= 2) 1 else 0,
            linkUrl = "/announcements/${event.announcementId}",
            payload = """{"announcementId":"${event.announcementId}"}""",
            ttlDays = 30,
            actor = event.publishedByName,
        )
    }
}
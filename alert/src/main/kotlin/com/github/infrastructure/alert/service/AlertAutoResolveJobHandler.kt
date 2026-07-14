package com.github.infrastructure.alert.service

import com.github.infrastructure.alert.config.AlertProperties
import com.github.infrastructure.alert.entity.AlertEvent
import com.github.infrastructure.alert.repository.AlertEventRepository
import com.github.infrastructure.scheduler.job.JobContext
import com.github.infrastructure.scheduler.job.JobHandler
import com.github.infrastructure.scheduler.job.Schedule
import java.time.Clock
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * JobHandler that auto-resolves idle alert events. Migrated from the
 * original `@Scheduled` component so it is now visible in /admin/jobs and
 * can be paused / triggered manually.
 *
 * Defaults to a 60-second fixed delay; admins can override via
 * `PUT /admin/jobs/alert-auto-resolve/schedule`.
 */
@Component
class AlertAutoResolveJobHandler(
    private val eventRepository: AlertEventRepository,
    private val properties: AlertProperties,
    private val clock: Clock,
) : JobHandler {

    override val code: String = "alert-auto-resolve"
    override val name: String = "Alert auto-resolve"
    override val description: String =
        "Marks alert events as resolved when they have been idle past the configured threshold"

    override fun defaultSchedule(): Schedule = Schedule.FixedDelay(seconds = 60)

    override fun handle(context: JobContext) {
        if (!properties.autoResolve.enabled) return
        val now = LocalDateTime.now(clock)
        val idleThreshold = now.minus(properties.autoResolve.idle)
        val idle = eventRepository.findIdleUnresolved(idleThreshold)
        if (idle.isEmpty()) return

        var resolvedCount = 0
        idle.forEach { event ->
            eventRepository.save(
                AlertEvent {
                    id = event.id
                    ruleId = event.ruleId
                    fingerprint = event.fingerprint
                    sourceModule = event.sourceModule
                    sourceAction = event.sourceAction
                    severity = event.severity
                    summary = event.summary
                    detail = event.detail
                    firstSeenAt = event.firstSeenAt
                    lastSeenAt = event.lastSeenAt
                    occurrences = event.occurrences
                    this.resolved = true
                    resolvedAt = now
                    createdTime = event.createdTime
                    updatedTime = now
                },
            )
            resolvedCount++
        }
        log.info("auto-resolved {} alert event(s) with no activity since {}", resolvedCount, idleThreshold)
    }

    companion object {
        private val log = LoggerFactory.getLogger(AlertAutoResolveJobHandler::class.java)
    }
}
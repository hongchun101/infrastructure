package com.github.infrastructure.alert.service

import com.github.infrastructure.alert.config.AlertProperties
import com.github.infrastructure.alert.entity.AlertEvent
import com.github.infrastructure.alert.repository.AlertEventRepository
import java.time.Clock
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AlertAutoResolveScheduler(
    private val eventRepository: AlertEventRepository,
    private val properties: AlertProperties,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "60000")
    @Transactional
    fun autoResolve() {
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
        if (resolvedCount > 0) {
            log.info("auto-resolved {} alert event(s) with no activity since {}", resolvedCount, idleThreshold)
        }
    }
}

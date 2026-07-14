package com.github.infrastructure.scheduler.service

import com.github.infrastructure.scheduler.config.SchedulerProperties
import com.github.infrastructure.scheduler.entity.JobDefinition
import com.github.infrastructure.scheduler.entity.JobExecution
import com.github.infrastructure.scheduler.entity.JobStatus
import com.github.infrastructure.scheduler.entity.JobTriggerType
import com.github.infrastructure.scheduler.entity.ScheduleType
import com.github.infrastructure.scheduler.job.CronExpression
import com.github.infrastructure.scheduler.job.JobHandler
import com.github.infrastructure.scheduler.job.JobRegistry
import com.github.infrastructure.scheduler.job.Schedule
import com.github.infrastructure.scheduler.repository.JobDefinitionRepository
import com.github.infrastructure.scheduler.repository.JobExecutionRepository
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Scans [JobDefinition] rows every second and creates [JobExecution]
 * rows whose [JobExecution.scheduledAt] is due. The runner service picks
 * them up separately so the scan loop never blocks on user code.
 *
 * On application start, also reconciles handler beans against the table:
 * inserts missing rows (enabled=false by default so admins explicitly
 * opt in) and logs definitions with no matching handler.
 */
@Service
class JobDispatcherService(
    private val properties: SchedulerProperties,
    private val definitionRepository: JobDefinitionRepository,
    private val executionRepository: JobExecutionRepository,
    private val registry: JobRegistry,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Reconcile handler beans into job_definitions. Idempotent: re-running on
     * every restart is safe. Existing rows are not overwritten so ops can
     * customize schedule/timeout without losing edits.
     */
    @Transactional
    fun reconcileDefinitions() {
        val existing = definitionRepository.listAll().associateBy { it.code }
        registry.all().forEach { handler ->
            val existingRow = existing[handler.code]
            if (existingRow == null) {
                insertDefinition(handler)
            } else {
                log.debug("job definition '{}' already exists, leaving untouched", handler.code)
            }
        }
        existing.values.filter { it.code !in registry.codes() }.forEach {
            log.warn("job definition '{}' has no JobHandler bean; it will be skipped", it.code)
        }
    }

    @Scheduled(fixedDelayString = "\${infrastructure.scheduler.scan-interval-ms:1000}")
    @Transactional
    fun scanAndEnqueue() {
        if (!properties.enabled) return
        val now = LocalDateTime.now(clock)
        val due = definitionRepository.listEnabled().filter { isDue(it, now) }
        for (def in due) {
            try {
                val nextRun = computeNextRun(def, now)
                executionRepository.save(
                    JobExecution {
                        jobId = def.id
                        status = JobStatus.PENDING
                        attempt = 1
                        triggerType = JobTriggerType.SCHEDULED
                        scheduledAt = now
                        nextRunAt = nextRun
                        createdAt = now
                    },
                )
                definitionRepository.save(def.copyWithLastRun(now, nextRun))
            } catch (e: Exception) {
                log.error("failed to enqueue job '{}': {}", def.code, e.message, e)
            }
        }
    }

    private fun isDue(def: JobDefinition, now: LocalDateTime): Boolean {
        val next = def.nextRunAt ?: return true
        return !next.isAfter(now)
    }

    fun computeNextRun(def: JobDefinition, from: LocalDateTime): LocalDateTime {
        val cron = def.cron
        if (cron != null) {
            val parser = CronExpression(cron)
            return parser.nextAfter(from)
        }
        val delay = def.fixedDelaySeconds
        if (delay != null) {
            return from.plus(Duration.ofSeconds(delay.toLong()))
        }
        throw IllegalStateException("job '${def.code}' has no schedule")
    }

    private fun insertDefinition(handler: JobHandler) {
        val now = LocalDateTime.now(clock)
        val (cron, delay) = when (val s = handler.defaultSchedule()) {
            is Schedule.Cron -> s.expression to null
            is Schedule.FixedDelay -> null to s.seconds
        }
        definitionRepository.save(
            JobDefinition {
                code = handler.code
                name = handler.name
                description = handler.description.takeIf { it.isNotBlank() }
                this.cron = cron
                fixedDelaySeconds = delay
                enabled = false
                retryMaxAttempts = properties.defaults.retryMaxAttempts
                retryInitialBackoffSeconds = properties.defaults.retryInitialBackoffSeconds
                retryMaxBackoffSeconds = properties.defaults.retryMaxBackoffSeconds
                retryMultiplier = properties.defaults.retryMultiplier
                timeoutSeconds = properties.defaults.timeoutSeconds
                payload = null
                lastFinishedAt = null
                lastRunAt = null
                nextRunAt = null
                createdAt = now
                updatedAt = now
            },
        )
        log.info("registered job definition '{}' (enabled=false by default)", handler.code)
    }

    private fun JobDefinition.copyWithLastRun(now: LocalDateTime, next: LocalDateTime): JobDefinition {
        // Reinsert via draft. We update last_run_at + next_run_at and bump updated_at.
        return JobDefinition {
            this.id = id
            code = this@copyWithLastRun.code
            name = this@copyWithLastRun.name
            description = this@copyWithLastRun.description
            cron = this@copyWithLastRun.cron
            fixedDelaySeconds = this@copyWithLastRun.fixedDelaySeconds
            enabled = this@copyWithLastRun.enabled
            retryMaxAttempts = this@copyWithLastRun.retryMaxAttempts
            retryInitialBackoffSeconds = this@copyWithLastRun.retryInitialBackoffSeconds
            retryMaxBackoffSeconds = this@copyWithLastRun.retryMaxBackoffSeconds
            retryMultiplier = this@copyWithLastRun.retryMultiplier
            timeoutSeconds = this@copyWithLastRun.timeoutSeconds
            payload = this@copyWithLastRun.payload
            lastFinishedAt = this@copyWithLastRun.lastFinishedAt
            lastRunAt = now
            nextRunAt = next
            createdAt = this@copyWithLastRun.createdAt
            updatedAt = now
        }
    }
}
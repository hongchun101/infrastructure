package com.github.infrastructure.scheduler.service

import com.github.infrastructure.scheduler.dto.JobDefinitionResponse
import com.github.infrastructure.scheduler.dto.JobExecutionResponse
import com.github.infrastructure.scheduler.dto.TriggerJobRequest
import com.github.infrastructure.scheduler.entity.JobDefinition
import com.github.infrastructure.scheduler.entity.JobExecution
import com.github.infrastructure.scheduler.entity.JobStatus
import com.github.infrastructure.scheduler.entity.JobTriggerType
import com.github.infrastructure.scheduler.error.JobErrors
import com.github.infrastructure.scheduler.job.Schedule
import com.github.infrastructure.scheduler.repository.JobDefinitionRepository
import com.github.infrastructure.scheduler.repository.JobExecutionRepository
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class JobAdminService(
    private val definitionRepository: JobDefinitionRepository,
    private val executionRepository: JobExecutionRepository,
    private val dispatcher: JobDispatcherService,
    private val clock: Clock,
) {
    fun list(): List<JobDefinitionResponse> = definitionRepository.listAll().map(JobDefinitionResponse::from)

    fun get(code: String): JobDefinitionResponse {
        val def = definitionRepository.findByCode(code) ?: throw JobErrors.notFound(code)
        return JobDefinitionResponse.from(def)
    }

    fun listRuns(code: String, limit: Int): List<JobExecutionResponse> {
        val def = definitionRepository.findByCode(code) ?: throw JobErrors.notFound(code)
        return executionRepository.listByJob(def.id, limit).map(JobExecutionResponse::from)
    }

    @Transactional
    fun pause(code: String): JobDefinitionResponse = setEnabled(code, false)

    @Transactional
    fun resume(code: String): JobDefinitionResponse {
        val def = definitionRepository.findByCode(code) ?: throw JobErrors.notFound(code)
        val now = LocalDateTime.now(clock)
        val next = if (def.nextRunAt == null) dispatcher.computeNextRun(def, now) else def.nextRunAt
        val updated = definitionRepository.save(
            JobDefinition {
                this.id = def.id
                this.code = def.code
                name = def.name
                description = def.description
                cron = def.cron
                fixedDelaySeconds = def.fixedDelaySeconds
                enabled = true
                retryMaxAttempts = def.retryMaxAttempts
                retryInitialBackoffSeconds = def.retryInitialBackoffSeconds
                retryMaxBackoffSeconds = def.retryMaxBackoffSeconds
                retryMultiplier = def.retryMultiplier
                timeoutSeconds = def.timeoutSeconds
                payload = def.payload
                lastFinishedAt = def.lastFinishedAt
                lastRunAt = def.lastRunAt
                this.nextRunAt = next
                createdAt = def.createdAt
                updatedAt = now
            },
        ).modifiedEntity
        return JobDefinitionResponse.from(updated)
    }

    @Transactional
    fun updateSchedule(code: String, schedule: Schedule): JobDefinitionResponse {
        val def = definitionRepository.findByCode(code) ?: throw JobErrors.notFound(code)
        val now = LocalDateTime.now(clock)
        val (cron, delay) = when (schedule) {
            is Schedule.Cron -> schedule.expression to null
            is Schedule.FixedDelay -> null to schedule.seconds
        }
        val updated = definitionRepository.save(
            JobDefinition {
                this.id = def.id
                this.code = def.code
                name = def.name
                description = def.description
                this.cron = cron
                this.fixedDelaySeconds = delay
                enabled = def.enabled
                retryMaxAttempts = def.retryMaxAttempts
                retryInitialBackoffSeconds = def.retryInitialBackoffSeconds
                retryMaxBackoffSeconds = def.retryMaxBackoffSeconds
                retryMultiplier = def.retryMultiplier
                timeoutSeconds = def.timeoutSeconds
                payload = def.payload
                lastFinishedAt = def.lastFinishedAt
                lastRunAt = def.lastRunAt
                nextRunAt = dispatcher.computeNextRun(
                    JobDefinition {
                        this.id = def.id
                        this.cron = cron
                        this.fixedDelaySeconds = delay
                    },
                    now,
                )
                createdAt = def.createdAt
                updatedAt = now
            },
        ).modifiedEntity
        return JobDefinitionResponse.from(updated)
    }

    @Transactional
    fun trigger(code: String, request: TriggerJobRequest): JobExecutionResponse {
        val def = definitionRepository.findByCode(code) ?: throw JobErrors.notFound(code)
        if (!def.enabled && request.requireEnabled) {
            throw JobErrors.disabled(code)
        }
        val now = LocalDateTime.now(clock)
        val execution = executionRepository.save(
            JobExecution {
                jobId = def.id
                status = JobStatus.PENDING
                attempt = 1
                triggerType = JobTriggerType.MANUAL
                scheduledAt = now
                startedAt = null
                finishedAt = null
                durationMs = null
                result = null
                error = null
                workerId = null
                nextRunAt = now
                createdAt = now
            },
        ).modifiedEntity
        return JobExecutionResponse.from(execution)
    }

    private fun setEnabled(code: String, enabled: Boolean): JobDefinitionResponse {
        val def = definitionRepository.findByCode(code) ?: throw JobErrors.notFound(code)
        val now = LocalDateTime.now(clock)
        val updated = definitionRepository.save(
            JobDefinition {
                this.id = def.id
                this.code = def.code
                name = def.name
                description = def.description
                cron = def.cron
                fixedDelaySeconds = def.fixedDelaySeconds
                this.enabled = enabled
                retryMaxAttempts = def.retryMaxAttempts
                retryInitialBackoffSeconds = def.retryInitialBackoffSeconds
                retryMaxBackoffSeconds = def.retryMaxBackoffSeconds
                retryMultiplier = def.retryMultiplier
                timeoutSeconds = def.timeoutSeconds
                payload = def.payload
                lastFinishedAt = def.lastFinishedAt
                lastRunAt = def.lastRunAt
                nextRunAt = def.nextRunAt
                createdAt = def.createdAt
                updatedAt = now
            },
        ).modifiedEntity
        return JobDefinitionResponse.from(updated)
    }
}

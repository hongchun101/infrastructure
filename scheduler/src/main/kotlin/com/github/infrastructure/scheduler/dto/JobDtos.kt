package com.github.infrastructure.scheduler.dto

import com.github.infrastructure.scheduler.entity.JobDefinition
import com.github.infrastructure.scheduler.entity.JobExecution
import com.github.infrastructure.scheduler.job.Schedule
import java.time.LocalDateTime

data class JobDefinitionResponse(
    val id: Long,
    val code: String,
    val name: String,
    val description: String?,
    val schedule: Schedule,
    val enabled: Boolean,
    val retryMaxAttempts: Int,
    val retryInitialBackoffSeconds: Long,
    val retryMaxBackoffSeconds: Long,
    val retryMultiplier: Double,
    val timeoutSeconds: Int,
    val lastRunAt: LocalDateTime?,
    val lastFinishedAt: LocalDateTime?,
    val nextRunAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(d: JobDefinition): JobDefinitionResponse {
            val schedule: Schedule = d.cron?.let { Schedule.Cron(it) }
                ?: d.fixedDelaySeconds?.let { Schedule.FixedDelay(it) }
                ?: throw IllegalStateException("job '${d.code}' has no schedule")
            return JobDefinitionResponse(
                id = d.id,
                code = d.code,
                name = d.name,
                description = d.description,
                schedule = schedule,
                enabled = d.enabled,
                retryMaxAttempts = d.retryMaxAttempts,
                retryInitialBackoffSeconds = d.retryInitialBackoffSeconds,
                retryMaxBackoffSeconds = d.retryMaxBackoffSeconds,
                retryMultiplier = d.retryMultiplier,
                timeoutSeconds = d.timeoutSeconds,
                lastRunAt = d.lastRunAt,
                lastFinishedAt = d.lastFinishedAt,
                nextRunAt = d.nextRunAt,
                createdAt = d.createdAt,
                updatedAt = d.updatedAt,
            )
        }
    }
}

data class JobExecutionResponse(
    val id: Long,
    val jobId: Long,
    val status: String,
    val attempt: Int,
    val triggerType: String,
    val scheduledAt: LocalDateTime,
    val startedAt: LocalDateTime?,
    val finishedAt: LocalDateTime?,
    val durationMs: Long?,
    val result: String?,
    val error: String?,
    val workerId: String?,
    val nextRunAt: LocalDateTime?,
) {
    companion object {
        fun from(e: JobExecution): JobExecutionResponse = JobExecutionResponse(
            id = e.id,
            jobId = e.jobId,
            status = e.status,
            attempt = e.attempt,
            triggerType = e.triggerType,
            scheduledAt = e.scheduledAt,
            startedAt = e.startedAt,
            finishedAt = e.finishedAt,
            durationMs = e.durationMs,
            result = e.result,
            error = e.error,
            workerId = e.workerId,
            nextRunAt = e.nextRunAt,
        )
    }
}

data class UpdateScheduleRequest(
    val cron: String? = null,
    val fixedDelaySeconds: Int? = null,
) {
    fun toSchedule(): Schedule = when {
        cron != null -> Schedule.Cron(cron)
        fixedDelaySeconds != null -> Schedule.FixedDelay(fixedDelaySeconds)
        else -> throw IllegalArgumentException("either cron or fixedDelaySeconds must be set")
    }
}

data class TriggerJobRequest(
    val requireEnabled: Boolean = true,
)
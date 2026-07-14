package com.github.infrastructure.scheduler.job

/**
 * Information passed to a [JobHandler.handle] invocation.
 *
 * @param jobId the [com.github.infrastructure.scheduler.entity.JobDefinition.id]
 * @param executionId the [com.github.infrastructure.scheduler.entity.JobExecution.id]
 * @param attempt 1 for the first try, 2+ for retries
 * @param payload optional JSON payload from job_definitions.payload
 * @param triggeredBy SCHEDULED, MANUAL, or RETRY
 */
data class JobContext(
    val jobId: Long,
    val executionId: Long,
    val attempt: Int,
    val payload: String?,
    val triggeredBy: String,
)
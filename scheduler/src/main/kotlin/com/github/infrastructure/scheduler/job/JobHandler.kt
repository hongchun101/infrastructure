package com.github.infrastructure.scheduler.job

/**
 * Business module implements this to register a schedulable unit of work. The
 * scheduler module picks the implementation up at startup, ensures a
 * [com.github.infrastructure.scheduler.entity.JobDefinition] row exists for
 * it, and calls [handle] when the schedule fires.
 *
 * The handler must be idempotent: the scheduler does not guarantee that
 * [handle] is called exactly once per trigger (a crash mid-execution will
 * mark the run FAILED and possibly retry). Treat every invocation as
 * "this might run twice".
 */
interface JobHandler {
    /** Stable identifier; matches job_definitions.code. */
    val code: String

    /** Human-readable name. Used when auto-creating the definition row. */
    val name: String

    /** One-line description. Optional. */
    val description: String get() = ""

    /**
     * Cron expression (6-field Quartz-style with seconds) OR a fixed delay
     * in seconds. Exactly one must be non-null on the
     * [com.github.infrastructure.scheduler.entity.JobDefinition] row.
     */
    fun defaultSchedule(): Schedule

    fun handle(context: JobContext)
}

sealed interface Schedule {
    data class Cron(val expression: String) : Schedule
    data class FixedDelay(val seconds: Int) : Schedule
}
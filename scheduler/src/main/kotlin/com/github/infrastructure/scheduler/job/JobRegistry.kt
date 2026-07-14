package com.github.infrastructure.scheduler.job

import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

/**
 * Discovers every [JobHandler] bean at startup. The dispatcher consults this
 * registry both to run jobs and to bootstrap missing
 * [com.github.infrastructure.scheduler.entity.JobDefinition] rows.
 *
 * Using [ObjectProvider] instead of constructor injection lets the scheduler
 * come up even when no handlers are registered (single-tenant apps).
 */
@Component
class JobRegistry(
    private val context: ApplicationContext,
) {
    private val byCode: Map<String, JobHandler> by lazy {
        context.getBeanProvider(JobHandler::class.java).orderedStream().toList().associateBy { it.code }
    }

    fun all(): List<JobHandler> = byCode.values.sortedBy { it.code }

    fun get(code: String): JobHandler? = byCode[code]

    fun codes(): Set<String> = byCode.keys
}
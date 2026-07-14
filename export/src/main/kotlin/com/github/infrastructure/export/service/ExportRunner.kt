package com.github.infrastructure.export.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.export.entity.ExportJob
import com.github.infrastructure.export.entity.ExportJobStatus
import com.github.infrastructure.export.handler.ExportFormat
import com.github.infrastructure.export.handler.ExportHandlerContext
import com.github.infrastructure.export.handler.ExportHandlerRegistry
import com.github.infrastructure.export.handler.ExportHandlerResult
import com.github.infrastructure.export.handler.ExportJobId
import com.github.infrastructure.export.repository.ExportJobRepository
import com.github.infrastructure.scheduler.job.JobContext
import com.github.infrastructure.scheduler.job.JobHandler
import com.github.infrastructure.scheduler.job.Schedule
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Scheduled job that picks up PENDING export_jobs and dispatches each to
 * its registered [com.github.infrastructure.export.handler.ExportHandler].
 *
 * The runner never reads or writes file bytes itself; it only:
 *   1. looks up the handler by [ExportJob.businessType],
 *   2. deserializes the params into the handler's parameter class,
 *   3. calls [ExportHandler.handle] and translates the returned
 *      [ExportHandlerResult] into an export_jobs row update.
 */
@Component
class ExportRunner(
    private val exportJobRepository: ExportJobRepository,
    private val registry: ExportHandlerRegistry,
    private val objectMapper: ObjectMapper,
    private val properties: com.github.infrastructure.export.config.ExportProperties,
    private val clock: Clock,
) : JobHandler {

    private val log = LoggerFactory.getLogger(javaClass)

    override val code: String = "export-runner"
    override val name: String = "Export job runner"
    override val description: String =
        "Dispatches queued export_jobs to the matching ExportHandler"

    override fun defaultSchedule(): Schedule = Schedule.FixedDelay(seconds = properties.runnerIntervalSeconds)

    override fun handle(ctx: JobContext) {
        val pending = exportJobRepository.listPending(properties.batchSize)
        for (job in pending) {
            try {
                runOne(job)
            } catch (e: Exception) {
                log.error("export job {} failed: {}", job.id, e.message, e)
                markFailed(job, e.message ?: e.javaClass.simpleName)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun runOne(job: ExportJob) {
        // Refuse to start a job that's been cancelled while we were waiting.
        val current = exportJobRepository.findActiveById(job.id) ?: return
        if (current.status != ExportJobStatus.PENDING) return

        val handler = registry.get(current.businessType)
        if (handler == null) {
            markFailed(current, "no handler for businessType='${current.businessType}'")
            return
        }

        val paramsJson = current.params ?: "{}"
        val params = try {
            objectMapper.readValue(paramsJson, handler.parameterClass().java)
        } catch (e: Exception) {
            markFailed(current, "failed to deserialize params: ${e.message}")
            return
        }

        val handlerCtx = ExportHandlerContext<Any>(
            exportJobId = ExportJobId(current.id),
            ownerUserId = current.ownerUserId,
            format = ExportFormat.fromString(current.format),
            params = params,
            fileName = current.fileName,
        )

        markRunning(current)

        val result = try {
            (handler as com.github.infrastructure.export.handler.ExportHandler<Any, Any>).handle(handlerCtx)
        } catch (e: Exception) {
            log.error("handler '{}' threw: {}", handler.type, e.message, e)
            ExportHandlerResult.Failed(e.message ?: e.javaClass.simpleName)
        }

        when (result) {
            is ExportHandlerResult.Success -> markSuccess(current, result)
            is ExportHandlerResult.Failed -> markFailed(current, result.message)
        }
    }

    @Transactional
    protected fun markRunning(job: ExportJob) {
        val now = LocalDateTime.now(clock)
        exportJobRepository.save(
            ExportJob {
                this.id = job.id
                service = job.service
                businessType = job.businessType
                format = job.format
                fileName = job.fileName
                params = job.params
                status = ExportJobStatus.RUNNING
                totalRows = job.totalRows
                processedRows = 0
                fileId = null
                error = null
                this.ownerUserId = job.ownerUserId
                startedAt = now
                finishedAt = null
                durationMs = null
                expiresAt = null
                createdAt = job.createdAt
                updatedAt = now
                this.deletedAt = job.deletedAt
            },
        )
    }

    @Transactional
    protected fun markSuccess(job: ExportJob, result: ExportHandlerResult.Success) {
        val now = LocalDateTime.now(clock)
        val expiresAt = result.expiresAt?.atZone(java.time.ZoneId.systemDefault())?.toLocalDateTime()
        val startedAt = job.startedAt ?: now
        exportJobRepository.save(
            ExportJob {
                this.id = job.id
                service = job.service
                businessType = job.businessType
                format = job.format
                fileName = job.fileName
                params = job.params
                status = ExportJobStatus.SUCCESS
                totalRows = result.totalRows
                processedRows = result.totalRows
                fileId = result.fileId
                error = null
                this.ownerUserId = job.ownerUserId
                this.startedAt = startedAt
                finishedAt = now
                durationMs = java.time.Duration.between(startedAt, now).toMillis()
                this.expiresAt = expiresAt
                createdAt = job.createdAt
                updatedAt = now
                this.deletedAt = job.deletedAt
            },
        )
    }

    @Transactional
    protected fun markFailed(job: ExportJob, message: String) {
        val now = LocalDateTime.now(clock)
        val startedAt = job.startedAt ?: now
        exportJobRepository.save(
            ExportJob {
                this.id = job.id
                service = job.service
                businessType = job.businessType
                format = job.format
                fileName = job.fileName
                params = job.params
                status = ExportJobStatus.FAILED
                totalRows = job.totalRows
                processedRows = job.processedRows
                fileId = job.fileId
                error = message.take(4000)
                this.ownerUserId = job.ownerUserId
                this.startedAt = startedAt
                finishedAt = now
                durationMs = java.time.Duration.between(startedAt, now).toMillis()
                expiresAt = null
                createdAt = job.createdAt
                updatedAt = now
                this.deletedAt = job.deletedAt
            },
        )
    }
}
package com.github.infrastructure.export.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.export.dto.CreateExportJobRequest
import com.github.infrastructure.export.dto.ExportJobResponse
import com.github.infrastructure.export.entity.ExportJob
import com.github.infrastructure.export.entity.ExportJobStatus
import com.github.infrastructure.export.error.ExportErrors
import com.github.infrastructure.export.handler.ExportFormat
import com.github.infrastructure.export.handler.ExportHandler
import com.github.infrastructure.export.handler.ExportHandlerRegistry
import com.github.infrastructure.export.repository.ExportJobRepository
import java.time.Clock
import java.time.LocalDateTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Orchestrates the export-job lifecycle from the client side: create, list,
 * get, cancel, retry, delete. The runner service owns the actual execution.
 *
 * Concurrency guard: a single user is limited to
 * [com.github.infrastructure.export.config.ExportProperties.maxConcurrentPerUser]
 * jobs in PENDING or RUNNING.
 */
@Service
class ExportService(
    private val repository: ExportJobRepository,
    private val registry: ExportHandlerRegistry,
    private val objectMapper: ObjectMapper,
    private val properties: com.github.infrastructure.export.config.ExportProperties,
    private val clock: Clock,
) {

    @Transactional
    fun create(request: CreateExportJobRequest, ownerUserId: java.util.UUID?): ExportJobResponse {
        val handler = registry.get(request.businessType)
            ?: throw ExportErrors.notFoundType(request.businessType)

        if (ownerUserId != null) {
            val active = repository.countActiveByOwner(ownerUserId)
            if (active >= properties.maxConcurrentPerUser) {
                throw ExportErrors.tooManyConcurrent(ownerUserId.toString())
            }
        }

        // Validate that the params can deserialize against the handler's parameter class.
        // We don't run the export; we just make sure a future runner call won't blow up.
        val paramsJson = request.params?.let { serializeForStorage(it) }
        paramsJson?.let { json ->
            runCatching { objectMapper.readValue(json, handler.parameterClass().java) }
                .onFailure { throw ExportErrors.badParams(it.message ?: it.javaClass.simpleName) }
        }

        val now = LocalDateTime.now(clock)
        val entity = repository.save(
            ExportJob {
                service = request.service
                businessType = request.businessType
                format = request.format.lowercase()
                fileName = request.fileName
                params = paramsJson
                status = ExportJobStatus.PENDING
                totalRows = null
                processedRows = 0
                fileId = null
                error = null
                this.ownerUserId = ownerUserId
                startedAt = null
                finishedAt = null
                durationMs = null
                expiresAt = null
                createdAt = now
                updatedAt = now
                deletedAt = null
            },
        ).modifiedEntity
        return ExportJobResponse.from(entity)
    }

    fun list(ownerUserId: java.util.UUID?, includeAll: Boolean): List<ExportJobResponse> {
        return if (includeAll) {
            // admin path: return recent regardless of owner
            repository.listAll().map(ExportJobResponse::from)
        } else {
            val owner = ownerUserId ?: throw ExportErrors.badParams("missing owner context")
            repository.listByOwner(owner, 100).map(ExportJobResponse::from)
        }
    }

    fun get(id: Long, ownerUserId: java.util.UUID?, isAdmin: Boolean): ExportJobResponse {
        val entity = repository.findActiveById(id) ?: throw ExportErrors.notFound(id)
        if (!isAdmin && entity.ownerUserId != ownerUserId) {
            throw ExportErrors.notFound(id)
        }
        return ExportJobResponse.from(entity)
    }

    @Transactional
    fun cancel(id: Long, ownerUserId: java.util.UUID?, isAdmin: Boolean): ExportJobResponse {
        val entity = repository.findActiveById(id) ?: throw ExportErrors.notFound(id)
        if (!isAdmin && entity.ownerUserId != ownerUserId) {
            throw ExportErrors.notFound(id)
        }
        if (entity.status == ExportJobStatus.SUCCESS ||
            entity.status == ExportJobStatus.FAILED ||
            entity.status == ExportJobStatus.CANCELLED
        ) {
            throw ExportErrors.alreadyTerminal(id, entity.status)
        }
        val now = LocalDateTime.now(clock)
        val updated = repository.save(
            ExportJob {
                this.id = entity.id
                service = entity.service
                businessType = entity.businessType
                format = entity.format
                fileName = entity.fileName
                params = entity.params
                status = ExportJobStatus.CANCELLED
                totalRows = entity.totalRows
                processedRows = entity.processedRows
                fileId = entity.fileId
                error = "cancelled by user"
                this.ownerUserId = entity.ownerUserId
                startedAt = entity.startedAt
                finishedAt = now
                durationMs = entity.startedAt?.let { java.time.Duration.between(it, now).toMillis() }
                expiresAt = entity.expiresAt
                createdAt = entity.createdAt
                updatedAt = now
                this.deletedAt = entity.deletedAt
            },
        ).modifiedEntity
        return ExportJobResponse.from(updated)
    }

    @Transactional
    fun retry(id: Long, ownerUserId: java.util.UUID?, isAdmin: Boolean): ExportJobResponse {
        val entity = repository.findActiveById(id) ?: throw ExportErrors.notFound(id)
        if (!isAdmin && entity.ownerUserId != ownerUserId) {
            throw ExportErrors.notFound(id)
        }
        if (entity.status != ExportJobStatus.FAILED) {
            throw ExportErrors.illegalState("only FAILED jobs can be retried, current status: ${entity.status}")
        }
        val now = LocalDateTime.now(clock)
        val updated = repository.save(
            ExportJob {
                this.id = entity.id
                service = entity.service
                businessType = entity.businessType
                format = entity.format
                fileName = entity.fileName
                params = entity.params
                status = ExportJobStatus.PENDING
                totalRows = null
                processedRows = 0
                fileId = null
                error = null
                this.ownerUserId = entity.ownerUserId
                startedAt = null
                finishedAt = null
                durationMs = null
                expiresAt = null
                createdAt = entity.createdAt
                updatedAt = now
                this.deletedAt = entity.deletedAt
            },
        ).modifiedEntity
        return ExportJobResponse.from(updated)
    }

    @Transactional
    fun delete(id: Long, ownerUserId: java.util.UUID?, isAdmin: Boolean) {
        val entity = repository.findActiveById(id) ?: throw ExportErrors.notFound(id)
        if (!isAdmin && entity.ownerUserId != ownerUserId) {
            throw ExportErrors.notFound(id)
        }
        val now = LocalDateTime.now(clock)
        repository.save(
            ExportJob {
                this.id = entity.id
                service = entity.service
                businessType = entity.businessType
                format = entity.format
                fileName = entity.fileName
                params = entity.params
                status = entity.status
                totalRows = entity.totalRows
                processedRows = entity.processedRows
                fileId = entity.fileId
                error = entity.error
                this.ownerUserId = entity.ownerUserId
                startedAt = entity.startedAt
                finishedAt = entity.finishedAt
                durationMs = entity.durationMs
                expiresAt = entity.expiresAt
                createdAt = entity.createdAt
                updatedAt = now
                deletedAt = now
            },
        )
    }

    private fun serializeForStorage(node: JsonNode): String = objectMapper.writeValueAsString(node)
}
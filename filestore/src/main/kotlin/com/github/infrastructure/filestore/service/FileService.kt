package com.github.infrastructure.filestore.service

import com.github.infrastructure.filestore.config.FilestoreProperties
import com.github.infrastructure.filestore.dto.ConfirmUploadRequest
import com.github.infrastructure.filestore.dto.DownloadTokenResponse
import com.github.infrastructure.filestore.dto.FileObjectResponse
import com.github.infrastructure.filestore.dto.RequestDownloadTokenRequest
import com.github.infrastructure.filestore.dto.RequestUploadTokenRequest
import com.github.infrastructure.filestore.dto.UploadTokenResponse
import com.github.infrastructure.filestore.entity.FileObject
import com.github.infrastructure.filestore.entity.FileObjectStatus
import com.github.infrastructure.filestore.entity.FileVisibility
import com.github.infrastructure.filestore.error.FileErrors
import com.github.infrastructure.filestore.repository.FileObjectRepository
import com.github.infrastructure.filestore.storage.PresignedDownload
import com.github.infrastructure.filestore.storage.PresignedUpload
import com.github.infrastructure.filestore.storage.StorageRegistry
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class FileService(
    private val props: FilestoreProperties,
    private val repository: FileObjectRepository,
    private val registry: StorageRegistry,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun requestUpload(
        request: RequestUploadTokenRequest,
        ownerUserId: UUID?,
    ): UploadTokenResponse {
        validate(request.contentType, request.sizeBytes, request.visibility)
        val targetBucket = request.bucket?.takeIf { it.isNotBlank() } ?: effectiveBucket()
        val storage = registry.resolve(targetBucket)
        val now = LocalDateTime.now(clock)
        val expiresAt = request.expiresInDays?.let { now.plusDays(it.toLong()) }
        val objectKey = generateObjectKey(request.originalName)
        val entity = repository.save(
            FileObject {
                bizType = request.bizType
                bizId = request.bizId
                bucket = targetBucket
                this.objectKey = objectKey
                originalName = request.originalName
                contentType = request.contentType
                sizeBytes = null
                sha256 = null
                storageProvider = storage.provider
                visibility = request.visibility
                status = FileObjectStatus.PENDING
                this.ownerUserId = ownerUserId
                metadata = null
                createdAt = now
                updatedAt = now
                uploadedAt = null
                this.expiresAt = expiresAt
                deletedAt = null
            },
        ).modifiedEntity
        val ttl = Duration.ofSeconds(
            (request.ttlSeconds ?: props.upload.defaultTtlSeconds)
                .coerceAtMost(props.upload.maxTtlSeconds),
        )
        val presigned: PresignedUpload = storage.presignUpload(
            bucket = targetBucket,
            key = objectKey,
            contentType = request.contentType,
            expiresIn = ttl,
            maxSizeBytes = props.upload.maxSizeBytes,
        )
        return UploadTokenResponse(
            fileId = entity.id,
            bucket = targetBucket,
            objectKey = objectKey,
            uploadUrl = presigned.url,
            method = presigned.method,
            headers = presigned.headers,
            expiresAt = presigned.expiresAt,
            maxSizeBytes = presigned.maxSizeBytes,
        )
    }

    @Transactional
    fun confirmUpload(
        fileId: Long,
        ownerUserId: UUID?,
        request: ConfirmUploadRequest,
    ): FileObjectResponse {
        val entity = repository.findActiveById(fileId)
            ?: throw FileErrors.notFound(fileId)
        if (ownerUserId != null && entity.ownerUserId != ownerUserId) {
            throw FileErrors.notOwner()
        }
        if (entity.status == FileObjectStatus.UPLOADED) {
            return FileObjectResponse.from(entity)
        }
        if (entity.status != FileObjectStatus.PENDING) {
            throw FileErrors.notPending(fileId, entity.status)
        }
        val storage = registry.resolve(entity.bucket)
        val stat = storage.stat(entity.bucket, entity.objectKey)
            ?: throw FileErrors.uploadMismatch(fileId, "object not found in storage")
        if (request.sizeBytes != null && request.sizeBytes != stat.sizeBytes) {
            throw FileErrors.uploadMismatch(fileId, "size mismatch ${stat.sizeBytes} vs ${request.sizeBytes}")
        }
        if (request.sha256 != null && entity.sha256 != null && request.sha256 != entity.sha256) {
            throw FileErrors.uploadMismatch(fileId, "sha256 mismatch")
        }
        val now = LocalDateTime.now(clock)
        val updated = repository.save(
            FileObject {
                this.id = entity.id
                bizType = entity.bizType
                bizId = entity.bizId
                bucket = entity.bucket
                objectKey = entity.objectKey
                originalName = entity.originalName
                contentType = entity.contentType
                sizeBytes = stat.sizeBytes
                sha256 = request.sha256 ?: entity.sha256
                storageProvider = entity.storageProvider
                visibility = entity.visibility
                status = FileObjectStatus.UPLOADED
                this.ownerUserId = entity.ownerUserId
                metadata = entity.metadata
                createdAt = entity.createdAt
                updatedAt = now
                uploadedAt = now
                expiresAt = entity.expiresAt
                deletedAt = entity.deletedAt
            },
        ).modifiedEntity
        return FileObjectResponse.from(updated)
    }

    @Transactional(readOnly = true)
    fun requestDownload(
        fileId: Long,
        ownerUserId: UUID?,
        request: RequestDownloadTokenRequest,
    ): DownloadTokenResponse {
        val entity = repository.findActiveById(fileId)
            ?: throw FileErrors.notFound(fileId)
        if (entity.status != FileObjectStatus.UPLOADED) {
            throw FileErrors.notPending(fileId, entity.status)
        }
        if (entity.visibility == FileVisibility.PRIVATE && entity.ownerUserId != ownerUserId) {
            throw FileErrors.notOwner()
        }
        val storage = registry.resolve(entity.bucket)
        val ttl = Duration.ofSeconds(
            (request.ttlSeconds ?: props.download.defaultTtlSeconds)
                .coerceAtMost(props.download.maxTtlSeconds),
        )
        val presigned: PresignedDownload = storage.presignDownload(entity.bucket, entity.objectKey, ttl)
        return DownloadTokenResponse(
            fileId = entity.id,
            bucket = entity.bucket,
            objectKey = entity.objectKey,
            downloadUrl = presigned.url,
            method = presigned.method,
            expiresAt = presigned.expiresAt,
        )
    }

    @Transactional
    fun delete(fileId: Long, ownerUserId: UUID?) {
        val entity = repository.findActiveById(fileId)
            ?: throw FileErrors.notFound(fileId)
        if (ownerUserId != null && entity.ownerUserId != ownerUserId) {
            throw FileErrors.notOwner()
        }
        val now = LocalDateTime.now(clock)
        repository.save(
            FileObject {
                this.id = entity.id
                bizType = entity.bizType
                bizId = entity.bizId
                bucket = entity.bucket
                objectKey = entity.objectKey
                originalName = entity.originalName
                contentType = entity.contentType
                sizeBytes = entity.sizeBytes
                sha256 = entity.sha256
                storageProvider = entity.storageProvider
                visibility = entity.visibility
                status = FileObjectStatus.DELETED
                this.ownerUserId = entity.ownerUserId
                metadata = entity.metadata
                createdAt = entity.createdAt
                updatedAt = now
                uploadedAt = entity.uploadedAt
                expiresAt = entity.expiresAt
                deletedAt = now
            },
        )
    }

    fun findById(fileId: Long): FileObjectResponse {
        val entity = repository.findActiveById(fileId)
            ?: throw FileErrors.notFound(fileId)
        return FileObjectResponse.from(entity)
    }

    private fun validate(contentType: String, sizeBytes: Long, visibility: String) {
        if (visibility != FileVisibility.PRIVATE && visibility != FileVisibility.PUBLIC) {
            throw FileErrors.invalidVisibility(visibility)
        }
        val maxSize = props.upload.maxSizeBytes
        if (sizeBytes > maxSize) throw FileErrors.tooLarge(maxSize, sizeBytes)
        if (props.upload.allowedContentTypes.isNotEmpty() &&
            contentType !in props.upload.allowedContentTypes
        ) {
            throw FileErrors.contentTypeDenied(contentType)
        }
    }

    private fun effectiveBucket(): String = when (registry.provider()) {
        "local" -> props.local.bucket
        else -> {
            val p = props.providerConfig(props.defaultProvider)
            p.bucket.ifBlank { props.defaultBucket }
        }
    }

    private fun generateObjectKey(originalName: String): String {
        val extension = originalName.substringAfterLast('.', "")
            .lowercase()
            .takeIf { it.length in 1..16 && it.all { ch -> ch.isLetterOrDigit() } }
            ?.let { ".$it" }
            ?: ""
        val now = Instant.now(clock)
        val date = now.atZone(java.time.ZoneId.of("UTC")).toLocalDate()
        return "${date.year}/${"%02d".format(date.monthValue)}/${UUID.randomUUID()}$extension"
    }
}
package com.github.infrastructure.filestore.dto

import com.github.infrastructure.filestore.entity.FileObject
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.Instant
import java.time.LocalDateTime
import java.util.UUID

data class RequestUploadTokenRequest(
    @field:NotBlank @field:Size(max = 64)
    val bizType: String,

    @field:Size(max = 64)
    val bizId: String? = null,

    @field:NotBlank @field:Size(max = 255)
    val originalName: String,

    @field:NotBlank @field:Pattern(regexp = "^[\\w.+-]+/[\\w.+-]+$")
    val contentType: String,

    @field:Min(1) @field:Max(5L * 1024 * 1024 * 1024)
    val sizeBytes: Long,

    @field:Size(max = 64)
    val bucket: String? = null,

    @field:Pattern(regexp = "PRIVATE|PUBLIC")
    val visibility: String = "PRIVATE",

    @field:Min(60) @field:Max(86400)
    val ttlSeconds: Long? = null,

    @field:Min(1) @field:Max(365)
    val expiresInDays: Int? = null,
)

data class UploadTokenResponse(
    val fileId: Long,
    val bucket: String,
    val objectKey: String,
    val uploadUrl: String,
    val method: String,
    val headers: Map<String, String>,
    val expiresAt: Instant,
    val maxSizeBytes: Long?,
)

data class ConfirmUploadRequest(
    @field:Size(min = 64, max = 64)
    val sha256: String? = null,

    @field:Min(1)
    val sizeBytes: Long? = null,
)

data class RequestDownloadTokenRequest(
    @field:Min(60)
    val ttlSeconds: Long? = null,
)

data class DownloadTokenResponse(
    val fileId: Long,
    val bucket: String,
    val objectKey: String,
    val downloadUrl: String,
    val method: String,
    val expiresAt: Instant,
)

data class FileObjectResponse(
    val id: Long,
    val bizType: String,
    val bizId: String?,
    val bucket: String,
    val objectKey: String,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long?,
    val sha256: String?,
    val storageProvider: String,
    val visibility: String,
    val status: String,
    val ownerUserId: UUID?,
    val createdAt: LocalDateTime,
    val uploadedAt: LocalDateTime?,
    val expiresAt: LocalDateTime?,
) {
    companion object {
        fun from(entity: FileObject): FileObjectResponse = FileObjectResponse(
            id = entity.id,
            bizType = entity.bizType,
            bizId = entity.bizId,
            bucket = entity.bucket,
            objectKey = entity.objectKey,
            originalName = entity.originalName,
            contentType = entity.contentType,
            sizeBytes = entity.sizeBytes,
            sha256 = entity.sha256,
            storageProvider = entity.storageProvider,
            visibility = entity.visibility,
            status = entity.status,
            ownerUserId = entity.ownerUserId,
            createdAt = entity.createdAt,
            uploadedAt = entity.uploadedAt,
            expiresAt = entity.expiresAt,
        )
    }
}
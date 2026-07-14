package com.github.infrastructure.filestore.storage

import java.io.InputStream
import java.time.Duration

/**
 * Provider-agnostic file storage contract. Implementations may use a remote
 * S3-compatible service (MinIO, AWS S3, Aliyun OSS, Tencent COS) or the
 * local filesystem.
 *
 * Read/write methods exist for the local-proxy endpoint only; object-storage
 * backends should throw [UnsupportedOperationException] from them because
 * clients go directly to the storage service.
 */
interface FileStorage {
    val provider: String

    fun presignUpload(
        bucket: String,
        key: String,
        contentType: String,
        expiresIn: Duration,
        maxSizeBytes: Long?,
    ): PresignedUpload

    fun presignDownload(
        bucket: String,
        key: String,
        expiresIn: Duration,
    ): PresignedDownload

    /**
     * Streamed write used by the local-proxy endpoint when a client uploads
     * to the application server. Object-storage backends throw.
     */
    fun write(
        bucket: String,
        key: String,
        contentType: String,
        input: InputStream,
        sizeHint: Long?,
    ): WriteResult

    fun openRead(bucket: String, key: String): InputStream

    fun stat(
        bucket: String,
        key: String,
    ): FileStat?

    fun delete(bucket: String, key: String)

    fun exists(bucket: String, key: String): Boolean = stat(bucket, key) != null
}

data class FileStat(
    val sizeBytes: Long,
    val contentType: String?,
    val etag: String?,
)
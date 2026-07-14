package com.github.infrastructure.filestore.storage

import java.time.Instant

/**
 * Presigned PUT instruction. The client PUTs the file body directly to [url]
 * with the supplied headers; the application server never sees the bytes.
 *
 * For storage backends that cannot truly presign (the local filesystem
 * backend), [url] points back at the application's own transfer endpoint,
 * which then forwards to the local disk. The contract is identical for
 * the client.
 */
data class PresignedUpload(
    val url: String,
    val method: String = "PUT",
    val headers: Map<String, String> = emptyMap(),
    val expiresAt: Instant,
    val maxSizeBytes: Long?,
)

data class PresignedDownload(
    val url: String,
    val method: String = "GET",
    val expiresAt: Instant,
)

data class WriteResult(
    val sizeBytes: Long,
    val sha256: String,
)
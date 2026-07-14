package com.github.infrastructure.export.handler

import java.time.Instant

/**
 * Outcome of [ExportHandler.handle]. The export service only inspects this
 * value: it never touches the file bytes, the upload URL, or the storage
 * backend. The handler is responsible for producing the file and returning
 * the resulting [com.github.infrastructure.filestore.entity.FileObject.id].
 */
sealed interface ExportHandlerResult {

    val totalRows: Long?

    data class Success(
        val fileId: Long,
        override val totalRows: Long,
        val expiresAt: Instant? = null,
    ) : ExportHandlerResult

    data class Failed(
        val message: String,
        override val totalRows: Long? = null,
    ) : ExportHandlerResult
}
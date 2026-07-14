package com.github.infrastructure.export.handler

import java.util.UUID

/**
 * Per-invocation context handed to [ExportHandler.handle].
 *
 * @param exportJobId  the row id of the export_jobs entry being processed
 * @param ownerUserId  the user who requested the export, for filestore ownership
 * @param format       XLSX or CSV
 * @param params       deserialized business parameters
 * @param fileName     caller-supplied or default base filename (no extension)
 */
data class ExportHandlerContext<PARAM : Any>(
    val exportJobId: ExportJobId,
    val ownerUserId: UUID?,
    val format: ExportFormat,
    val params: PARAM,
    val fileName: String,
)
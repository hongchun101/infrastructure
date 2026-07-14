package com.github.infrastructure.export.dto

import com.fasterxml.jackson.databind.JsonNode
import com.github.infrastructure.export.entity.ExportJob
import com.github.infrastructure.export.handler.ExportFormat
import com.github.infrastructure.export.handler.ExportHandler
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateExportJobRequest(
    @field:Size(max = 64)
    val service: String = "export",

    @field:NotBlank @field:Size(max = 64)
    val businessType: String,

    @field:Pattern(regexp = "xlsx|csv", flags = [Pattern.Flag.CASE_INSENSITIVE])
    val format: String = "xlsx",

    @field:NotBlank @field:Size(max = 255)
    val fileName: String,

    val params: JsonNode? = null,
)

data class ExportJobResponse(
    val id: Long,
    val service: String,
    val businessType: String,
    val format: String,
    val fileName: String,
    val status: String,
    val totalRows: Long?,
    val processedRows: Long,
    val fileId: Long?,
    val error: String?,
    val startedAt: LocalDateTime?,
    val finishedAt: LocalDateTime?,
    val durationMs: Long?,
    val expiresAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(j: ExportJob): ExportJobResponse = ExportJobResponse(
            id = j.id,
            service = j.service,
            businessType = j.businessType,
            format = j.format,
            fileName = j.fileName,
            status = j.status,
            totalRows = j.totalRows,
            processedRows = j.processedRows,
            fileId = j.fileId,
            error = j.error,
            startedAt = j.startedAt,
            finishedAt = j.finishedAt,
            durationMs = j.durationMs,
            expiresAt = j.expiresAt,
            createdAt = j.createdAt,
        )
    }
}

data class ExportHandlerSummary(
    val type: String,
    val displayName: String,
    val sheetName: String,
    val parameterClass: String,
    val columns: List<String>,
) {
    companion object {
        fun from(h: ExportHandler<*, *>): ExportHandlerSummary = ExportHandlerSummary(
            type = h.type,
            displayName = h.displayName,
            sheetName = h.sheetName,
            parameterClass = h.parameterClass().qualifiedName ?: h.parameterClass().toString(),
            columns = h.columns.map { it.header },
        )
    }
}

data class CreateExportJobResponse(
    val id: Long,
    val status: String,
)

internal fun ExportFormat.toEntityValue(): String = name.lowercase()
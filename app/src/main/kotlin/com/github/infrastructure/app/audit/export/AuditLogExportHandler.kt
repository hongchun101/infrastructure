package com.github.infrastructure.app.audit.export

import com.github.infrastructure.app.audit.entity.OperationLogEntry
import com.github.infrastructure.app.audit.repository.OperationLogRepository
import com.github.infrastructure.export.handler.CellFormat
import com.github.infrastructure.export.handler.ColumnSpec
import com.github.infrastructure.export.handler.ExportFormat
import com.github.infrastructure.export.handler.ExportHandler
import com.github.infrastructure.export.handler.ExportHandlerContext
import com.github.infrastructure.export.handler.ExportHandlerResult
import com.github.infrastructure.export.handler.ExportJobId
import com.github.infrastructure.export.workbook.CsvWriter
import com.github.infrastructure.export.workbook.UploadClient
import com.github.infrastructure.export.workbook.WorkbookBuilder
import com.github.infrastructure.filestore.dto.ConfirmUploadRequest
import com.github.infrastructure.filestore.dto.RequestUploadTokenRequest
import com.github.infrastructure.filestore.dto.UploadTokenResponse
import com.github.infrastructure.filestore.service.FileService
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Demo ExportHandler. Streams every matching operation-log row into a
 * workbook (or CSV) and ships the bytes through the filestore upload-token
 * pipeline; the export service only sees the resulting [ExportHandlerResult].
 */
@Component
class AuditLogExportHandler(
    private val operationLogRepository: OperationLogRepository,
    private val fileService: FileService,
    private val uploadClient: UploadClient,
) : ExportHandler<AuditLogExportParams, OperationLogEntry> {

    override val type: String = "audit-logs"
    override val displayName: String = "Audit Logs"
    override val sheetName: String = "Audit"
    override fun parameterClass(): KClass<AuditLogExportParams> = AuditLogExportParams::class

    override val columns: List<ColumnSpec<OperationLogEntry>> = listOf(
        ColumnSpec("Time", OperationLogEntry::createdTime, CellFormat.DateTimeFormat()),
        ColumnSpec("Trace", OperationLogEntry::traceId),
        ColumnSpec("User", OperationLogEntry::username),
        ColumnSpec("Module", OperationLogEntry::module),
        ColumnSpec("Action", OperationLogEntry::action),
        ColumnSpec("Description", OperationLogEntry::description),
        ColumnSpec("Method", OperationLogEntry::method),
        ColumnSpec("Path", OperationLogEntry::path),
        ColumnSpec("Status", OperationLogEntry::responseStatus),
        ColumnSpec("Duration (ms)", OperationLogEntry::durationMs),
        ColumnSpec("Result", OperationLogEntry::success, CellFormat.BooleanFormat("OK", "FAIL")),
        ColumnSpec("Error", OperationLogEntry::errorMessage),
    )

    override fun totalRows(params: AuditLogExportParams): Long =
        operationLogRepository.countForExport(
            module = params.module,
            action = params.action,
            userId = params.userId,
            success = params.success,
            startTime = params.startTime,
            endTime = params.endTime,
        )

    override fun fetchPage(offset: Int, limit: Int, params: AuditLogExportParams): List<OperationLogEntry> =
        operationLogRepository.listForExport(
            module = params.module,
            action = params.action,
            userId = params.userId,
            success = params.success,
            startTime = params.startTime,
            endTime = params.endTime,
            offset = offset,
            limit = limit,
        )

    override fun handle(ctx: ExportHandlerContext<AuditLogExportParams>): ExportHandlerResult {
        val maxBytes = MAX_EXPORT_SIZE_BYTES
        val token = fileService.requestUpload(
            RequestUploadTokenRequest(
                bizType = "export",
                bizId = ctx.exportJobId.value.toString(),
                originalName = "${ctx.fileName}.${ctx.format.ext}",
                contentType = ctx.format.contentType,
                sizeBytes = maxBytes,
                visibility = "PRIVATE",
                ttlSeconds = 3600,
            ),
            ownerUserId = ctx.ownerUserId,
        )

        val totalRows = try {
            when (ctx.format) {
                ExportFormat.XLSX -> writeXlsx(token, ctx)
                ExportFormat.CSV -> writeCsv(token, ctx)
            }
        } catch (e: Exception) {
            log.warn("export {} failed, rolling back upload: {}", ctx.exportJobId, e.message)
            runCatching { fileService.delete(token.fileId, ctx.ownerUserId) }
            return ExportHandlerResult.Failed(e.message ?: e.javaClass.simpleName)
        }

        runCatching {
            fileService.confirmUpload(token.fileId, ctx.ownerUserId, ConfirmUploadRequest())
        }.onFailure {
            runCatching { fileService.delete(token.fileId, ctx.ownerUserId) }
            return ExportHandlerResult.Failed("confirm upload failed: ${it.message}")
        }

        return ExportHandlerResult.Success(
            fileId = token.fileId,
            totalRows = totalRows,
            expiresAt = Instant.now().plus(7, ChronoUnit.DAYS),
        )
    }

    private fun writeXlsx(token: UploadTokenResponse, ctx: ExportHandlerContext<AuditLogExportParams>): Long {
        val buffer = ByteArrayOutputStream(4 * 1024 * 1024)
        val totalRows = WorkbookBuilder(buffer, sheetName, columns).use { wb ->
            streamRows(ctx) { page -> wb.appendRows(page) }
        }
        uploadClient.put(token.uploadUrl, token.method, token.headers, buffer.toByteArray())
        return totalRows
    }

    private fun writeCsv(token: UploadTokenResponse, ctx: ExportHandlerContext<AuditLogExportParams>): Long {
        val buffer = ByteArrayOutputStream(64 * 1024)
        val totalRows = CsvWriter(buffer, columns).use { csv ->
            streamRows(ctx) { page -> csv.appendRows(page) }
        }
        uploadClient.put(token.uploadUrl, token.method, token.headers, buffer.toByteArray())
        return totalRows
    }

    private fun streamRows(
        ctx: ExportHandlerContext<AuditLogExportParams>,
        pageSize: Int = 5000,
        consume: (List<OperationLogEntry>) -> Unit,
    ): Long {
        var total = 0L
        var offset = 0
        while (true) {
            val page = fetchPage(offset, pageSize, ctx.params)
            if (page.isEmpty()) break
            consume(page)
            total += page.size
            offset += page.size
            if (page.size < pageSize) break
        }
        return total
    }

    companion object {
        private val log = LoggerFactory.getLogger(AuditLogExportHandler::class.java)
        private const val MAX_EXPORT_SIZE_BYTES = 200L * 1024 * 1024
    }
}
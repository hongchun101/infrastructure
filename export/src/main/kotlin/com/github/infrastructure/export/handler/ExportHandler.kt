package com.github.infrastructure.export.handler

import kotlin.reflect.KClass

/**
 * Contract for a business export.
 *
 * The handler is the only place that knows how to read data and shape it
 * into a file; the export service dispatches to it and accepts the resulting
 * [ExportHandlerResult] without inspecting the bytes. Implementations
 * typically:
 *
 *  1. ask [com.github.infrastructure.filestore.service.FileService] for an
 *     upload token,
 *  2. stream the rows from [fetchPage] into a workbook / csv,
 *  3. PUT the bytes to the token's URL (or write them to local disk and let
 *     the filestore local backend forward),
 *  4. call `fileService.confirmUpload(...)` and return
 *     [ExportHandlerResult.Success] with the resulting fileId,
 *  5. on any failure, call `fileService.delete(...)` on the orphan upload
 *     and return [ExportHandlerResult.Failed] (or throw — the runner catches
 *     and translates).
 *
 * @param PARAM  business parameter type; the runner uses [parameterClass] to
 *               deserialize the JSON stored in `export_jobs.params`.
 * @param ROW    row representation; one Excel/CSV line per instance.
 */
interface ExportHandler<PARAM : Any, ROW : Any> {
    val type: String
    val displayName: String
    val sheetName: String

    fun parameterClass(): KClass<PARAM>

    val columns: List<ColumnSpec<ROW>>

    fun totalRows(params: PARAM): Long? = null

    fun fetchPage(offset: Int, limit: Int, params: PARAM): List<ROW>

    fun handle(ctx: ExportHandlerContext<PARAM>): ExportHandlerResult
}
package com.github.infrastructure.export.workbook

import com.github.infrastructure.export.handler.CellFormat
import com.github.infrastructure.export.handler.ColumnSpec
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicInteger
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.xssf.streaming.SXSSFCell
import org.apache.poi.xssf.streaming.SXSSFRow
import org.apache.poi.xssf.streaming.SXSSFSheet
import org.apache.poi.xssf.streaming.SXSSFWorkbook

/**
 * Streaming-friendly wrapper around [SXSSFWorkbook] that knows how to write
 * a header row plus an arbitrary sequence of body rows.
 *
 * Memory: only the most recent 100 rows are held in RAM (the SXSSF window);
 * the rest is flushed to temp files and reassembled during [close].
 */
class WorkbookBuilder(
    private val out: OutputStream,
    sheetName: String,
    private val columns: List<ColumnSpec<*>>,
    private val rowAccessWindowSize: Int = 100,
) : AutoCloseable {

    private val workbook: SXSSFWorkbook = SXSSFWorkbook(rowAccessWindowSize)
    private val sheet: SXSSFSheet = workbook.createSheet(sheetName)
    private val rowIndex: AtomicInteger = AtomicInteger(0)
    private var closed = false

    init {
        writeHeader()
    }

    fun writeHeader() {
        val row: SXSSFRow = sheet.createRow(rowIndex.getAndIncrement())
        val headerStyle = workbook.createCellStyle().apply {
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }
        columns.forEachIndexed { i, col ->
            val cell = row.createCell(i)
            cell.setCellValue(col.header)
            cell.cellStyle = headerStyle
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <ROW> appendRow(row: ROW) {
        val poiRow: SXSSFRow = sheet.createRow(rowIndex.getAndIncrement())
        columns.forEachIndexed { i, colRaw ->
            val col = colRaw as ColumnSpec<ROW>
            val cell = poiRow.createCell(i)
            writeCell(cell, col.extractor(row), col.format)
        }
    }

    fun <ROW> appendRows(rows: Iterable<ROW>) {
        rows.forEach { appendRow(it) }
    }

    override fun close() {
        if (closed) return
        closed = true
        try {
            workbook.use { it.write(out) }
        } finally {
            workbook.dispose()
        }
    }

    private fun writeCell(cell: SXSSFCell, value: Any?, format: CellFormat) {
        when (format) {
            CellFormat.StringFormat ->
                cell.setCellValue(value?.toString() ?: "")

            is CellFormat.DateTimeFormat -> {
                val pattern = DateTimeFormatter.ofPattern(format.pattern)
                val text = when (value) {
                    null -> ""
                    is LocalDateTime -> value.format(pattern)
                    is Instant -> value.atZone(ZoneId.systemDefault()).toLocalDateTime().format(pattern)
                    is java.util.Date -> value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(pattern)
                    else -> value.toString()
                }
                cell.setCellValue(text)
            }

            is CellFormat.DateFormat -> {
                val pattern = DateTimeFormatter.ofPattern(format.pattern)
                val text = when (value) {
                    null -> ""
                    is LocalDate -> value.format(pattern)
                    is LocalDateTime -> value.toLocalDate().format(pattern)
                    is Instant -> value.atZone(ZoneId.systemDefault()).toLocalDate().format(pattern)
                    else -> value.toString()
                }
                cell.setCellValue(text)
            }

            is CellFormat.NumberFormat -> when (value) {
                null -> cell.setCellValue("")
                is Number -> cell.setCellValue(value.toDouble())
                else -> cell.setCellValue(value.toString())
            }

            is CellFormat.BooleanFormat -> when (value) {
                is Boolean -> cell.setCellValue(if (value) format.yes else format.no)
                null -> cell.setCellValue("")
                else -> cell.setCellValue(value.toString())
            }

            is CellFormat.EnumFormat -> {
                val key = value?.toString() ?: ""
                cell.setCellValue(format.mapping[key] ?: key)
            }
        }
    }
}
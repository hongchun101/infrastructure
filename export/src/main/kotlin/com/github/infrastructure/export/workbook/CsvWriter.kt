package com.github.infrastructure.export.workbook

import com.github.infrastructure.export.handler.CellFormat
import com.github.infrastructure.export.handler.ColumnSpec
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * RFC 4180 CSV writer with a UTF-8 BOM prefix. Lightweight: no external
 * library, no streaming window.
 */
class CsvWriter(
    private val out: OutputStream,
    private val columns: List<ColumnSpec<*>>,
) : AutoCloseable {

    private val writer = out.bufferedWriter(Charsets.UTF_8)
    private var closed = false
    private val rowSeparator = "\r\n"

    init {
        // Excel-friendly BOM so that opening the file directly shows UTF-8.
        out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
        writeRow(columns.map { it.header })
    }

    @Suppress("UNCHECKED_CAST")
    fun <ROW> appendRow(row: ROW) {
        writeRow(columns.map { colRaw ->
            val col = colRaw as ColumnSpec<ROW>
            renderCell(col.extractor(row), col.format)
        })
    }

    fun <ROW> appendRows(rows: Iterable<ROW>) {
        rows.forEach { appendRow(it) }
    }

    private fun writeRow(cells: List<String>) {
        writer.write(cells.joinToString(",") { escape(it) })
        writer.write(rowSeparator)
    }

    private fun escape(value: String): String {
        val needsQuote = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuote) "\"$escaped\"" else escaped
    }

    private fun renderCell(value: Any?, format: CellFormat): String {
        return when (format) {
            CellFormat.StringFormat -> value?.toString() ?: ""
            is CellFormat.DateTimeFormat -> {
                val pattern = DateTimeFormatter.ofPattern(format.pattern)
                when (value) {
                    null -> ""
                    is LocalDateTime -> value.format(pattern)
                    is Instant -> value.atZone(ZoneId.systemDefault()).toLocalDateTime().format(pattern)
                    is java.util.Date -> value.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(pattern)
                    else -> value.toString()
                }
            }
            is CellFormat.DateFormat -> {
                val pattern = DateTimeFormatter.ofPattern(format.pattern)
                when (value) {
                    null -> ""
                    is LocalDate -> value.format(pattern)
                    is LocalDateTime -> value.toLocalDate().format(pattern)
                    is Instant -> value.atZone(ZoneId.systemDefault()).toLocalDate().format(pattern)
                    else -> value.toString()
                }
            }
            is CellFormat.NumberFormat -> when (value) {
                null -> ""
                is Number -> format.pattern.let { p ->
                    runCatching { java.text.DecimalFormat(p).format(value) }
                        .getOrDefault(value.toString())
                }
                else -> value.toString()
            }
            is CellFormat.BooleanFormat -> when (value) {
                is Boolean -> if (value) format.yes else format.no
                null -> ""
                else -> value.toString()
            }
            is CellFormat.EnumFormat -> {
                val key = value?.toString() ?: ""
                format.mapping[key] ?: key
            }
        }
    }

    override fun close() {
        if (closed) return
        closed = true
        writer.flush()
    }
}
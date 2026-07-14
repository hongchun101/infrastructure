package com.github.infrastructure.export.workbook

import com.github.infrastructure.export.handler.CellFormat
import com.github.infrastructure.export.handler.ColumnSpec
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

class CsvWriterTest {

    private data class Row(val name: String, val note: String)

    private val columns = listOf(
        ColumnSpec("Name", Row::name),
        ColumnSpec("Note", Row::note),
    )

    @Test
    fun `emits utf8 bom and headers`() {
        val buffer = ByteArrayOutputStream()
        CsvWriter(buffer, columns).use { /* no rows */ }
        val bytes = buffer.toByteArray()
        // UTF-8 BOM
        assertThat(bytes[0].toInt() and 0xFF).isEqualTo(0xEF)
        assertThat(bytes[1].toInt() and 0xFF).isEqualTo(0xBB)
        assertThat(bytes[2].toInt() and 0xFF).isEqualTo(0xBF)
        val text = String(bytes, Charsets.UTF_8).removePrefix("\uFEFF")
        assertThat(text).contains("Name,Note")
    }

    @Test
    fun `quotes and escapes commas and quotes per rfc 4180`() {
        val buffer = ByteArrayOutputStream()
        CsvWriter(buffer, columns).use { csv ->
            csv.appendRow(Row("alice", "hello, world"))
            csv.appendRow(Row("bob", "she said \"hi\""))
        }
        val text = String(buffer.toByteArray(), Charsets.UTF_8).removePrefix("\uFEFF")
        assertThat(text).contains("\"hello, world\"")
        assertThat(text).contains("\"she said \"\"hi\"\"\"")
    }

    @Test
    fun `renders multiple rows`() {
        val buffer = ByteArrayOutputStream()
        CsvWriter(buffer, columns).use { csv ->
            csv.appendRows(listOf(Row("alice", "ok"), Row("bob", "fine")))
        }
        val text = String(buffer.toByteArray(), Charsets.UTF_8).removePrefix("\uFEFF")
        val lines = text.split("\r\n").filter { it.isNotEmpty() }
        assertThat(lines).hasSize(3) // header + 2 rows
        assertThat(lines[1]).isEqualTo("alice,ok")
        assertThat(lines[2]).isEqualTo("bob,fine")
    }
}
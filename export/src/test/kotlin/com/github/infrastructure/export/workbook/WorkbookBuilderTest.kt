package com.github.infrastructure.export.workbook

import com.github.infrastructure.export.handler.CellFormat
import com.github.infrastructure.export.handler.ColumnSpec
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.time.LocalDateTime

class WorkbookBuilderTest {

    private data class Row(val name: String, val amount: Int, val at: LocalDateTime, val active: Boolean)

    private val columns = listOf(
        ColumnSpec("Name", Row::name),
        ColumnSpec("Amount", Row::amount, CellFormat.NumberFormat("#,##0")),
        ColumnSpec("Date", Row::at, CellFormat.DateTimeFormat("yyyy-MM-dd")),
        ColumnSpec("Active", Row::active, CellFormat.BooleanFormat("Y", "N")),
    )

    @Test
    fun `writes a valid xlsx zip`() {
        val buffer = ByteArrayOutputStream()
        val rows = listOf(
            Row("alice", 100, LocalDateTime.of(2026, 7, 14, 10, 30), true),
            Row("bob", 250, LocalDateTime.of(2026, 7, 14, 11, 0), false),
        )
        WorkbookBuilder(buffer, "Sheet1", columns).use { wb ->
            wb.appendRows(rows)
        }

        val bytes = buffer.toByteArray()
        assertThat(bytes).isNotEmpty
        // XLSX is a ZIP; the first 4 bytes should be 'PK\003\004'
        assertThat(bytes[0].toInt().toChar()).isEqualTo('P')
        assertThat(bytes[1].toInt().toChar()).isEqualTo('K')
    }

    @Test
    fun `date only format works`() {
        val buffer = ByteArrayOutputStream()
        val cols = listOf(
            ColumnSpec("Date", { it: LocalDate -> it }, CellFormat.DateFormat()),
        )
        WorkbookBuilder(buffer, "Dates", cols).use { wb ->
            wb.appendRow(LocalDate.of(2026, 7, 14))
        }
        assertThat(buffer.toByteArray()).isNotEmpty
    }

    @Test
    fun `empty body still produces valid xlsx`() {
        val buffer = ByteArrayOutputStream()
        WorkbookBuilder(buffer, "Empty", columns).use { /* no rows */ }
        val bytes = buffer.toByteArray()
        assertThat(bytes.size).isGreaterThan(0)
    }
}
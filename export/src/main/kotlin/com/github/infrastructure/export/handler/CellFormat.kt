package com.github.infrastructure.export.handler

/**
 * Styling for a single cell value when written to XLSX / CSV.
 */
sealed interface CellFormat {

    data object StringFormat : CellFormat

    data class DateTimeFormat(
        val pattern: String = "yyyy-MM-dd HH:mm:ss",
    ) : CellFormat

    data class DateFormat(
        val pattern: String = "yyyy-MM-dd",
    ) : CellFormat

    data class NumberFormat(
        val pattern: String = "#,##0.##",
    ) : CellFormat

    data class BooleanFormat(
        val yes: String = "Yes",
        val no: String = "No",
    ) : CellFormat

    /**
     * Maps an enum/constant value to its display label. Unmapped values fall
     * back to [String.toString].
     */
    data class EnumFormat(
        val mapping: Map<String, String>,
    ) : CellFormat
}
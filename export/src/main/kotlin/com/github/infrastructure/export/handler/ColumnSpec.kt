package com.github.infrastructure.export.handler

import kotlin.reflect.KProperty1

/**
 * Single column declaration: how to render one value of [ROW] into a sheet
 * cell.
 *
 * Two construction styles are supported:
 *   ColumnSpec("Name", OperationLog::username)                      // property ref
 *   ColumnSpec("Name", { it.username }, CellFormat.StringFormat)    // lambda + format
 */
data class ColumnSpec<ROW>(
    val header: String,
    val extractor: (ROW) -> Any?,
    val format: CellFormat = CellFormat.StringFormat,
) {
    companion object {
        /**
         * Convenience constructor that takes a property reference. Resolves
         * to a getter lambda at construction time so call sites read like a
         * record declaration.
         */
        operator fun <ROW> invoke(
            header: String,
            reference: KProperty1<ROW, *>,
            format: CellFormat = CellFormat.StringFormat,
        ): ColumnSpec<ROW> = ColumnSpec(header, { reference.get(it) }, format)
    }
}
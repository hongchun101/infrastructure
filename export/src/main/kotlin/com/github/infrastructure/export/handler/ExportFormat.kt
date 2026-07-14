package com.github.infrastructure.export.handler

/**
 * File formats supported by the export pipeline. Each value carries the
 * default MIME type and the canonical extension used when assembling the
 * uploaded object's original filename.
 */
enum class ExportFormat(val ext: String, val contentType: String) {
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    CSV("csv", "text/csv"),
    ;

    companion object {
        fun fromString(raw: String): ExportFormat =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) }
                ?: throw IllegalArgumentException("unknown export format '$raw'")
    }
}
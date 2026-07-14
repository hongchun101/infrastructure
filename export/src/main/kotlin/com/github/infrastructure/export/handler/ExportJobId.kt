package com.github.infrastructure.export.handler

/**
 * Strongly typed wrapper around the database-generated id so callers can't
 * accidentally pass an unrelated [Long] where an export-job id is expected.
 */
@JvmInline
value class ExportJobId(val value: Long) {
    override fun toString(): String = value.toString()
}
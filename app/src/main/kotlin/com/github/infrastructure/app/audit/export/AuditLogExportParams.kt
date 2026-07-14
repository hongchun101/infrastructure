package com.github.infrastructure.app.audit.export

import com.fasterxml.jackson.annotation.JsonInclude
import java.time.LocalDateTime
import java.util.UUID

/**
 * Business parameter shape for the audit-log export. Mirrors the filter
 * fields on the in-app OperationLog query so users can narrow the export
 * the same way they narrow the live list.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class AuditLogExportParams(
    val module: String? = null,
    val action: String? = null,
    val userId: UUID? = null,
    val success: Boolean? = null,
    val startTime: LocalDateTime? = null,
    val endTime: LocalDateTime? = null,
)
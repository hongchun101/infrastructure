package com.github.infrastructure.app.audit

import java.time.LocalDateTime
import java.util.UUID

data class OperationLogResponse(
    val id: UUID,
    val traceId: String?,
    val userId: UUID?,
    val username: String?,
    val module: String,
    val action: String,
    val description: String?,
    val method: String,
    val path: String,
    val queryString: String?,
    val responseStatus: Int?,
    val errorMessage: String?,
    val clientIp: String?,
    val userAgent: String?,
    val durationMs: Long,
    val success: Boolean,
    val createdTime: LocalDateTime,
)
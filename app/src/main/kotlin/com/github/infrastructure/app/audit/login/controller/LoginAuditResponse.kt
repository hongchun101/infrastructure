package com.github.infrastructure.app.audit.login.controller

import java.time.LocalDateTime
import java.util.UUID

data class LoginAuditResponse(
    val id: UUID,
    val accountType: String,
    val loginMode: String?,
    val principal: String?,
    val accountId: UUID?,
    val username: String?,
    val outcome: String,
    val failureReason: String?,
    val clientIp: String?,
    val userAgent: String?,
    val traceId: String?,
    val createdTime: LocalDateTime,
)

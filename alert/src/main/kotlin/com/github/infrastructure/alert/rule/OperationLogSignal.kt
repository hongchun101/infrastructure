package com.github.infrastructure.alert.rule

import java.util.UUID

/**
 * Platform-neutral signal consumed by every [AlertRuleMatcher]. It is produced
 * by the integration layer (see the bridge in the app module) so that the
 * alert module itself has no compile-time dependency on the audit module.
 */
data class OperationLogSignal(
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
    val responseStatus: Int,
    val errorMessage: String?,
    val clientIp: String?,
    val userAgent: String?,
    val durationMs: Long,
    val success: Boolean,
)

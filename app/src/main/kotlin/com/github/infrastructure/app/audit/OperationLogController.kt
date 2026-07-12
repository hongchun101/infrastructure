package com.github.infrastructure.app.audit

import java.time.LocalDateTime
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class OperationLogController(
    private val operationLogQueryService: OperationLogQueryService,
) {
    @GetMapping("/operation-logs")
    @PreAuthorize("@permissionChecker.has('operation:log:read')")
    fun list(
        @RequestParam(required = false) module: String?,
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) userId: UUID?,
        @RequestParam(required = false) success: Boolean?,
        @RequestParam(required = false) startTime: LocalDateTime?,
        @RequestParam(required = false) endTime: LocalDateTime?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<OperationLogResponse> = operationLogQueryService.list(
        module,
        action,
        userId,
        success,
        startTime,
        endTime,
        page,
        size,
    )
}
package com.github.infrastructure.app.audit.controller

import com.github.infrastructure.app.audit.OperationLogResponse
import com.github.infrastructure.app.audit.PageResponse
import com.github.infrastructure.app.audit.service.OperationLogQueryService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

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

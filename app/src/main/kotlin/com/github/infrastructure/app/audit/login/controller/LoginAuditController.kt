package com.github.infrastructure.app.audit.login.controller

import com.github.infrastructure.app.audit.dto.PageResponse
import com.github.infrastructure.app.audit.login.service.LoginAuditService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
class LoginAuditController(
    private val loginAuditService: LoginAuditService,
) {
    @GetMapping("/login-audits")
    @PreAuthorize("@permissionChecker.has('login:audit:read')")
    fun list(
        @RequestParam(required = false) accountType: String?,
        @RequestParam(required = false) outcome: String?,
        @RequestParam(required = false) principal: String?,
        @RequestParam(required = false) accountId: UUID?,
        @RequestParam(required = false) startTime: LocalDateTime?,
        @RequestParam(required = false) endTime: LocalDateTime?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<LoginAuditResponse> = loginAuditService.list(
        accountType,
        outcome,
        principal,
        accountId,
        startTime,
        endTime,
        page,
        size,
    )
}

package com.github.infrastructure.app.audit.login.service
import com.github.infrastructure.app.audit.dto.PageResponse
import com.github.infrastructure.app.audit.login.controller.LoginAuditResponse
import com.github.infrastructure.app.audit.login.repository.LoginAuditEntry
import com.github.infrastructure.app.audit.login.repository.LoginAuditRepository
import com.github.infrastructure.core.web.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class LoginAuditService(
    private val loginAuditRepository: LoginAuditRepository,
) {
    fun list(
        accountType: String?,
        outcome: String?,
        principal: String?,
        accountId: UUID?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        page: Int,
        size: Int,
    ): PageResponse<LoginAuditResponse> {
        if (page < 0) {
            throw BusinessException(HttpStatus.BAD_REQUEST.value(), "page must be >= 0", HttpStatus.BAD_REQUEST)
        }
        if (size <= 0 || size > MAX_PAGE_SIZE) {
            throw BusinessException(
                HttpStatus.BAD_REQUEST.value(),
                "size must be in (0, $MAX_PAGE_SIZE]",
                HttpStatus.BAD_REQUEST,
            )
        }
        val result = loginAuditRepository.findPage(
            accountType = accountType,
            outcome = outcome,
            principal = principal,
            accountId = accountId,
            startTime = startTime,
            endTime = endTime,
            pageIndex = page,
            pageSize = size,
        )
        return PageResponse(
            items = result.rows.map { it.toResponse() },
            total = result.totalRowCount,
            page = page,
            size = size,
        )
    }

    private fun LoginAuditEntry.toResponse(): LoginAuditResponse = LoginAuditResponse(
        id = id,
        accountType = accountType,
        loginMode = loginMode,
        principal = principal,
        accountId = accountId,
        username = username,
        outcome = outcome,
        failureReason = failureReason,
        clientIp = clientIp,
        userAgent = userAgent,
        traceId = traceId,
        createdTime = createdTime,
    )

    companion object {
        private const val MAX_PAGE_SIZE = 200
    }
}

package com.github.infrastructure.app.audit.service

import com.github.infrastructure.app.audit.dto.OperationLogResponse
import com.github.infrastructure.app.audit.dto.PageResponse
import com.github.infrastructure.app.audit.entity.OperationLogEntry
import com.github.infrastructure.app.audit.repository.OperationLogRepository
import com.github.infrastructure.core.web.exception.BusinessException
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.UUID

@Service
class OperationLogQueryService(
    private val operationLogRepository: OperationLogRepository,
) {
    fun list(
        module: String?,
        action: String?,
        userId: UUID?,
        success: Boolean?,
        startTime: LocalDateTime?,
        endTime: LocalDateTime?,
        page: Int,
        size: Int,
    ): PageResponse<OperationLogResponse> {
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
        val result = operationLogRepository.findPage(
            module = module,
            action = action,
            userId = userId,
            success = success,
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

    private fun OperationLogEntry.toResponse(): OperationLogResponse = OperationLogResponse(
        id = id,
        traceId = traceId,
        userId = userId,
        username = username,
        module = module,
        action = action,
        description = description,
        method = method,
        path = path,
        queryString = queryString,
        responseStatus = responseStatus,
        errorMessage = errorMessage,
        clientIp = clientIp,
        userAgent = userAgent,
        durationMs = durationMs,
        success = success,
        createdTime = createdTime,
    )

    companion object {
        private const val MAX_PAGE_SIZE = 200
    }
}

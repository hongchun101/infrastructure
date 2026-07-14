package com.github.infrastructure.alert.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.alert.dto.AlertNotificationResponse
import com.github.infrastructure.alert.dto.PageResponse
import com.github.infrastructure.alert.entity.AlertNotification
import com.github.infrastructure.alert.entity.AlertNotificationStatus
import com.github.infrastructure.alert.repository.AlertEventRepository
import com.github.infrastructure.alert.repository.AlertNotificationRepository
import com.github.infrastructure.core.web.exception.BusinessException
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class AlertNotificationService(
    private val notificationRepository: AlertNotificationRepository,
    private val eventRepository: AlertEventRepository,
    private val objectMapper: ObjectMapper,
) {
    fun list(eventId: UUID, page: Int, size: Int): PageResponse<AlertNotificationResponse> {
        if (page < 0) badRequest("page must be >= 0")
        if (size <= 0 || size > MAX_PAGE_SIZE) badRequest("size must be in (0, $MAX_PAGE_SIZE]")
        if (eventRepository.findById(eventId) == null) throw eventNotFound(eventId)
        val result = notificationRepository.findPageByEventId(eventId, page, size)
        return PageResponse(
            items = result.rows.map { it.toResponse() },
            total = result.totalRowCount,
            page = page,
            size = size,
        )
    }

    private fun badRequest(message: String) {
        throw BusinessException(HttpStatus.BAD_REQUEST.value(), message, HttpStatus.BAD_REQUEST)
    }

    private fun eventNotFound(id: UUID): BusinessException =
        BusinessException(HttpStatus.NOT_FOUND.value(), "alert event $id not found", HttpStatus.NOT_FOUND)

    private fun AlertNotification.toResponse(): AlertNotificationResponse {
        val payloadNode: JsonNode? = payload?.takeIf { it.isNotBlank() }?.let { objectMapper.readTree(it) }
        return AlertNotificationResponse(
            id = id,
            eventId = eventId,
            channel = channel,
            target = target,
            status = AlertNotificationStatus.valueOf(status),
            httpStatus = httpStatus,
            errorMessage = errorMessage,
            payload = payloadNode,
            sentAt = sentAt,
            createdTime = createdTime,
        )
    }

    companion object {
        private const val MAX_PAGE_SIZE = 200
    }
}

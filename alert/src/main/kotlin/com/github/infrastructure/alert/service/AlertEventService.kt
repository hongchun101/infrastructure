package com.github.infrastructure.alert.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.alert.dto.AlertEventResponse
import com.github.infrastructure.alert.dto.PageResponse
import com.github.infrastructure.alert.entity.AlertEvent
import com.github.infrastructure.alert.entity.Severity
import com.github.infrastructure.alert.repository.AlertEventRepository
import com.github.infrastructure.alert.repository.AlertRuleRepository
import com.github.infrastructure.core.web.exception.BusinessException
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlertEventService(
    private val eventRepository: AlertEventRepository,
    private val ruleRepository: AlertRuleRepository,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    fun list(
        ruleId: UUID?,
        severity: String?,
        resolved: Boolean?,
        page: Int,
        size: Int,
    ): PageResponse<AlertEventResponse> {
        if (page < 0) badRequest("page must be >= 0")
        if (size <= 0 || size > MAX_PAGE_SIZE) badRequest("size must be in (0, $MAX_PAGE_SIZE]")
        val result = eventRepository.findPage(ruleId, severity, resolved, page, size)
        val rulesById = ruleRepository.findAllEnabled().associateBy { it.id }
        return PageResponse(
            items = result.rows.map { it.toResponse(rulesById[it.ruleId]) },
            total = result.totalRowCount,
            page = page,
            size = size,
        )
    }

    fun get(id: UUID): AlertEventResponse {
        val event = eventRepository.findById(id) ?: throw notFound(id)
        val rule = ruleRepository.findById(event.ruleId)
        return event.toResponse(rule)
    }

    @Transactional
    fun resolve(id: UUID): AlertEventResponse {
        val current = eventRepository.findById(id) ?: throw notFound(id)
        if (current.resolved) return get(id)
        val now = LocalDateTime.now(clock)
        eventRepository.save(
            AlertEvent {
                this.id = current.id
                ruleId = current.ruleId
                fingerprint = current.fingerprint
                sourceModule = current.sourceModule
                sourceAction = current.sourceAction
                severity = current.severity
                summary = current.summary
                detail = current.detail
                firstSeenAt = current.firstSeenAt
                lastSeenAt = current.lastSeenAt
                occurrences = current.occurrences
                resolved = true
                resolvedAt = now
                createdTime = current.createdTime
                updatedTime = now
            },
        )
        return get(id)
    }

    private fun badRequest(message: String) {
        throw BusinessException(HttpStatus.BAD_REQUEST.value(), message, HttpStatus.BAD_REQUEST)
    }

    private fun notFound(id: UUID): BusinessException =
        BusinessException(HttpStatus.NOT_FOUND.value(), "alert event $id not found", HttpStatus.NOT_FOUND)

    private fun AlertEvent.toResponse(rule: com.github.infrastructure.alert.entity.AlertRule?): AlertEventResponse {
        val detailNode = detail?.takeIf { it.isNotBlank() }?.let { objectMapper.readTree(it) }
        return AlertEventResponse(
            id = id,
            ruleId = ruleId,
            ruleCode = rule?.code,
            ruleName = rule?.name,
            fingerprint = fingerprint,
            sourceModule = sourceModule,
            sourceAction = sourceAction,
            severity = Severity.valueOf(severity),
            summary = summary,
            detail = detailNode,
            firstSeenAt = firstSeenAt,
            lastSeenAt = lastSeenAt,
            occurrences = occurrences,
            resolved = resolved,
            resolvedAt = resolvedAt,
            createdTime = createdTime,
            updatedTime = updatedTime,
        )
    }

    companion object {
        private const val MAX_PAGE_SIZE = 200
    }
}

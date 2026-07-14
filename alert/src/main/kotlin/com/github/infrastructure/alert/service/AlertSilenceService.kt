package com.github.infrastructure.alert.service

import com.github.infrastructure.alert.dto.AlertSilenceResponse
import com.github.infrastructure.alert.dto.CreateAlertSilenceRequest
import com.github.infrastructure.alert.entity.AlertSilence
import com.github.infrastructure.alert.repository.AlertSilenceRepository
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AlertSilenceService(
    private val silenceRepository: AlertSilenceRepository,
    private val clock: Clock,
) {
    @Transactional
    fun create(request: CreateAlertSilenceRequest, createdBy: String?): AlertSilenceResponse {
        require(request.endsAt.isAfter(request.startsAt)) {
            "endsAt must be after startsAt"
        }
        val now = LocalDateTime.now(clock)
        val saved = silenceRepository.save(
            AlertSilence {
                name = request.name
                ruleId = request.ruleId
                startsAt = request.startsAt
                endsAt = request.endsAt
                reason = request.reason
                active = true
                this.createdBy = createdBy
                createdAt = now
                updatedAt = now
            },
        ).modifiedEntity
        return AlertSilenceResponse.from(saved)
    }

    @Transactional
    fun deactivate(id: Long): AlertSilenceResponse {
        val silence = silenceRepository.findById(id)
            ?: throw NoSuchElementException("alert silence $id not found")
        if (!silence.active) {
            return AlertSilenceResponse.from(silence)
        }
        val now = LocalDateTime.now(clock)
        val updated = silenceRepository.save(
            AlertSilence {
                this.id = silence.id
                name = silence.name
                ruleId = silence.ruleId
                startsAt = silence.startsAt
                endsAt = silence.endsAt
                reason = silence.reason
                active = false
                createdBy = silence.createdBy
                createdAt = silence.createdAt
                updatedAt = now
            },
        ).modifiedEntity
        return AlertSilenceResponse.from(updated)
    }

    fun isSilenced(ruleId: UUID, at: LocalDateTime): Boolean =
        silenceRepository.isSilenced(ruleId, at)

    fun listActive(): List<AlertSilenceResponse> =
        silenceRepository.listActive(LocalDateTime.now(clock))
            .map(AlertSilenceResponse::from)

    fun listAll(): List<AlertSilenceResponse> =
        silenceRepository.listAll().map(AlertSilenceResponse::from)
}
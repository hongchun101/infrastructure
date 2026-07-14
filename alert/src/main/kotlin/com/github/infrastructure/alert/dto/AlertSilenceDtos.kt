package com.github.infrastructure.alert.dto

import com.github.infrastructure.alert.entity.AlertSilence
import jakarta.validation.constraints.Future
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateAlertSilenceRequest(
    @field:NotBlank @field:Size(max = 255)
    val name: String,

    val ruleId: UUID? = null,

    @field:NotNull
    val startsAt: LocalDateTime,

    @field:NotNull
    @field:Future
    val endsAt: LocalDateTime,

    @field:Size(max = 500)
    val reason: String? = null,
)

data class AlertSilenceResponse(
    val id: Long,
    val name: String,
    val ruleId: UUID?,
    val startsAt: LocalDateTime,
    val endsAt: LocalDateTime,
    val reason: String?,
    val active: Boolean,
    val createdBy: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(silence: AlertSilence): AlertSilenceResponse = AlertSilenceResponse(
            id = silence.id,
            name = silence.name,
            ruleId = silence.ruleId,
            startsAt = silence.startsAt,
            endsAt = silence.endsAt,
            reason = silence.reason,
            active = silence.active,
            createdBy = silence.createdBy,
            createdAt = silence.createdAt,
            updatedAt = silence.updatedAt,
        )
    }
}
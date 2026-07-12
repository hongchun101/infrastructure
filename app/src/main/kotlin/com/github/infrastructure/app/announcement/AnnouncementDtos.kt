package com.github.infrastructure.app.announcement

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateAnnouncementRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:Size(max = 500)
    val summary: String? = null,
    @field:NotBlank
    val content: String,
    @field:Min(0)
    @field:Max(9)
    val priority: Int = 0,
)

data class UpdateAnnouncementRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:Size(max = 500)
    val summary: String?,
    @field:NotBlank
    val content: String,
    @field:Min(0)
    @field:Max(9)
    val priority: Int,
)

data class ScheduleAnnouncementRequest(
    @field:NotNull
    val publishAt: LocalDateTime,
)

data class AnnouncementResponse(
    val id: UUID,
    val title: String,
    val summary: String?,
    val content: String,
    val status: AnnouncementStatus,
    val priority: Int,
    val publishedAt: LocalDateTime?,
    val publishAt: LocalDateTime?,
    val readCount: Int,
    val readByMe: Boolean,
    val createdBy: UUID,
    val updatedBy: UUID,
    val createdTime: LocalDateTime,
    val updatedTime: LocalDateTime,
)

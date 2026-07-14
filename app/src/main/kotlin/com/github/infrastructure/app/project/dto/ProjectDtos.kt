package com.github.infrastructure.app.project.dto

import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime
import java.util.UUID

data class CreateProjectRequest(
    @field:NotBlank
    val name: String,
)

data class ProjectResponse(
    val id: UUID,
    val name: String,
    val ownerId: UUID,
    val createdTime: LocalDateTime,
)

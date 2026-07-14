package com.github.infrastructure.app.user.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateBackendAccountRequest(
    @field:NotBlank @field:Size(max = 100) val username: String,
    @field:Size(max = 255) val email: String? = null,
    @field:Size(max = 32) val phone: String? = null,
    @field:NotBlank @field:Size(min = 8, max = 100) val password: String,
    @field:NotBlank @field:Size(max = 100) val displayName: String,
    val enabled: Boolean = true,
    val roleIds: Set<UUID> = emptySet(),
)

data class BackendAccountResponse(
    val id: UUID,
    val username: String,
    val email: String?,
    val phone: String?,
    val displayName: String,
    val enabled: Boolean,
    val roleIds: Set<UUID>,
    val createdTime: LocalDateTime,
    val updatedTime: LocalDateTime,
)

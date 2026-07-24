package com.github.infrastructure.app.user.profile

import com.github.infrastructure.security.auth.AccountType
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class UpdateProfileRequest(
    @field:Size(max = 100)
    val displayName: String? = null,
    @field:Size(max = 255)
    @field:Email
    val email: String? = null,
    @field:Size(max = 32)
    val phone: String? = null,
)

data class ChangePasswordRequest(
    @field:NotBlank
    val currentPassword: String,
    @field:NotBlank
    @field:Size(min = 8, max = 100)
    val newPassword: String,
)

data class ProfileResponse(
    val id: UUID,
    val accountType: AccountType,
    val username: String,
    val displayName: String,
    val email: String?,
    val phone: String?,
    val updatedTime: LocalDateTime?,
)

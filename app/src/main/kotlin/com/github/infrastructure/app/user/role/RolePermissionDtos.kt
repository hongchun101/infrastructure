package com.github.infrastructure.app.user.role

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDateTime
import java.util.UUID

data class CreateRoleRequest(
    @field:NotBlank
    @field:Size(max = 100)
    @field:Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "code must be uppercase letters/digits/underscore")
    val code: String,
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
    val permissionIds: Set<UUID> = emptySet(),
)

data class UpdateRoleRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
    val permissionIds: Set<UUID> = emptySet(),
)

data class RoleResponse(
    val id: UUID,
    val code: String,
    val name: String,
    val permissions: List<PermissionSummary>,
    val createdTime: LocalDateTime,
)

data class PermissionSummary(
    val id: UUID,
    val code: String,
    val name: String,
)

data class CreatePermissionRequest(
    @field:NotBlank
    @field:Size(max = 100)
    @field:Pattern(regexp = "^[a-z][a-z0-9:_]*$", message = "code must be lowercase with colon or underscore")
    val code: String,
    @field:NotBlank
    @field:Size(max = 100)
    val name: String,
)

data class PermissionResponse(
    val id: UUID,
    val code: String,
    val name: String,
)

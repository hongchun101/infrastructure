package com.github.infrastructure.security.user

import java.util.UUID

data class SecurityUserAccount(
    val id: UUID,
    val username: String,
    val passwordHash: String,
    val displayName: String,
    val enabled: Boolean,
    val roles: List<String>,
    val permissions: List<String>,
)

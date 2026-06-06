package com.github.infrastructure.security

import java.util.UUID

data class AuthenticatedUser(
    val id: UUID,
    val username: String,
    val displayName: String,
    val roles: List<String>,
    val permissions: List<String>,
)

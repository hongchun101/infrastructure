package com.github.infrastructure.security.context

import java.util.UUID

data class AuthenticatedUser(
    val id: UUID,
    val username: String,
    val displayName: String,
    val roles: List<String>,
    val permissions: List<String>,
    val accountType: com.github.infrastructure.security.auth.AccountType = com.github.infrastructure.security.auth.AccountType.USER,
)

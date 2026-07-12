package com.github.infrastructure.security.token

import com.github.infrastructure.security.context.AuthenticatedUser
import java.time.Instant

data class TokenSession(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthenticatedUser,
    val issuedAt: Instant,
    val accessTokenExpiresAt: Instant,
    val refreshTokenExpiresAt: Instant,
)

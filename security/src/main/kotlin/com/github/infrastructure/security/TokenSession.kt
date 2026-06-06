package com.github.infrastructure.security

import java.time.Instant

data class TokenSession(
    val accessToken: String,
    val refreshToken: String,
    val user: AuthenticatedUser,
    val issuedAt: Instant,
    val accessTokenExpiresAt: Instant,
    val refreshTokenExpiresAt: Instant,
)

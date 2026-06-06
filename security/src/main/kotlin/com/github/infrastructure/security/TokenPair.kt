package com.github.infrastructure.security

data class TokenPair(
    val accessToken: String,
    val refreshToken: String,
    val accessTokenExpiresInSeconds: Long,
    val refreshTokenExpiresInSeconds: Long,
)

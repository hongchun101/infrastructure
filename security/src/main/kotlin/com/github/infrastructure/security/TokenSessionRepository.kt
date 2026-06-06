package com.github.infrastructure.security

import java.time.Duration

interface TokenSessionRepository {
    fun save(session: TokenSession, accessTokenTtl: Duration, refreshTokenTtl: Duration)

    fun findByAccessToken(accessToken: String): TokenSession?

    fun findByRefreshToken(refreshToken: String): TokenSession?

    fun delete(session: TokenSession)
}

package com.github.infrastructure.security

import org.springframework.data.redis.core.RedisTemplate
import java.time.Duration

class RedisTokenSessionRepository(
    private val redisTemplate: RedisTemplate<String, TokenSession>,
) : TokenSessionRepository {
    override fun save(session: TokenSession, accessTokenTtl: Duration, refreshTokenTtl: Duration) {
        redisTemplate.opsForValue().set(accessKey(session.accessToken), session, accessTokenTtl)
        redisTemplate.opsForValue().set(refreshKey(session.refreshToken), session, refreshTokenTtl)
    }

    override fun findByAccessToken(accessToken: String): TokenSession? =
        redisTemplate.opsForValue().get(accessKey(accessToken))

    override fun findByRefreshToken(refreshToken: String): TokenSession? =
        redisTemplate.opsForValue().get(refreshKey(refreshToken))

    override fun delete(session: TokenSession) {
        redisTemplate.delete(listOf(accessKey(session.accessToken), refreshKey(session.refreshToken)))
    }

    private fun accessKey(token: String): String = "infrastructure:security:access:$token"

    private fun refreshKey(token: String): String = "infrastructure:security:refresh:$token"
}

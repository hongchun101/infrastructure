package com.github.infrastructure.security.service

import com.github.infrastructure.security.config.SecurityProperties
import java.security.MessageDigest
import java.time.Clock
import java.time.Duration
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * Sliding-window login attempt tracker, backed by Redis counter with a TTL.
 *
 * The window is approximated by setting a TTL on the counter equal to the
 * configured login rate-limit window: every attempt increments the counter and
 * resets its TTL. When the counter exceeds [SecurityProperties.LoginRateLimit.maxAttemptsPerPrincipal],
 * subsequent login attempts for the same principal are blocked until the
 * counter expires.
 */
@Component
class LoginRateLimiter(
    private val redisTemplate: StringRedisTemplate,
    private val properties: SecurityProperties,
    private val clock: Clock,
) {
    open fun isAllowed(principal: String): Boolean {
        if (!properties.loginRateLimit.enabled) return true
        val key = key(principal)
        val count = redisTemplate.opsForValue().increment(key, 1) ?: 0
        if (count == 1L) {
            redisTemplate.expire(key, properties.loginRateLimit.window)
        }
        return count <= properties.loginRateLimit.maxAttemptsPerPrincipal
    }

    open fun recordSuccess(principal: String) {
        if (!properties.loginRateLimit.enabled) return
        redisTemplate.delete(key(principal))
    }

    open fun ttl(principal: String): Duration = runCatching {
        val millis = redisTemplate.getExpire(key(principal), java.util.concurrent.TimeUnit.MILLISECONDS)
        if (millis == null || millis < 0) Duration.ZERO else Duration.ofMillis(millis)
    }.getOrDefault(Duration.ZERO)

    private fun key(principal: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(principal.lowercase().toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }.take(32)
        return "auth:login:attempts:$hex"
    }
}

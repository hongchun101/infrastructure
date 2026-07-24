package com.github.infrastructure.security.auth

import com.github.infrastructure.core.web.exception.BusinessException
import com.github.infrastructure.security.config.SecurityProperties
import com.github.infrastructure.security.password.PasswordHasher
import com.github.infrastructure.security.repository.SecurityUserAccountRepository
import com.github.infrastructure.security.service.AuthService
import com.github.infrastructure.security.service.LoginRateLimiter
import com.github.infrastructure.security.token.TokenSession
import com.github.infrastructure.security.token.TokenSessionRepository
import com.github.infrastructure.security.token.UuidTokenGenerator
import com.github.infrastructure.security.user.SecurityUserAccount
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.ArrayDeque
import java.util.UUID
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.HttpStatus

class AuthServiceTest {
    private val userId = UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val clock = Clock.fixed(Instant.parse("2026-06-06T00:00:00Z"), ZoneOffset.UTC)
    @Test
    fun `login stores access and refresh tokens with session data`() {
        val tokens = ArrayDeque(listOf("access-1", "refresh-1"))
        val sessionRepository = InMemoryTokenSessionRepository()
        val service = authService(tokens, sessionRepository)
        val response = service.login(LoginRequest("admin", "secret"))
        assertEquals("access-1", response.accessToken)
        assertEquals("refresh-1", response.refreshToken)
        assertEquals(1800, response.accessTokenExpiresInSeconds)
        assertEquals(604800, response.refreshTokenExpiresInSeconds)
        val accessSession = sessionRepository.findAccess("access-1")
        val refreshSession = sessionRepository.findRefresh("refresh-1")
        assertNotNull(accessSession)
        assertNotNull(refreshSession)
        assertEquals(userId, accessSession!!.user.id)
        assertEquals(listOf("ADMIN"), accessSession.user.roles)
        assertEquals(listOf("project:read", "project:write"), accessSession.user.permissions)
        assertEquals("refresh-1", accessSession.refreshToken)
    }
    @Test
    fun `login supports email and phone modes`() {
        val tokens = ArrayDeque(listOf("email-access", "email-refresh", "phone-access", "phone-refresh"))
        val sessionRepository = InMemoryTokenSessionRepository()
        val service = authService(tokens, sessionRepository)

        val emailLogin = service.login(LoginRequest(mode = LoginMode.EMAIL, principal = "admin@example.com", password = "secret"))
        val phoneLogin = service.login(LoginRequest(mode = LoginMode.PHONE, principal = "13800000000", password = "secret"))

        assertEquals("email-access", emailLogin.accessToken)
        assertEquals("phone-access", phoneLogin.accessToken)
    }

    @Test
    fun `legacy username login request remains supported`() {
        val service = authService(ArrayDeque(listOf("access-1", "refresh-1")), InMemoryTokenSessionRepository())

        val response = service.login(LoginRequest(username = "admin", password = "secret"))

        assertEquals("access-1", response.accessToken)
    }

    @Test
    fun `login rejects blank principal`() {
        val service = authService(ArrayDeque(listOf("access-1", "refresh-1")), InMemoryTokenSessionRepository())

        val exception = assertThrows(BusinessException::class.java) {
            service.login(LoginRequest(mode = LoginMode.EMAIL, principal = " ", password = "secret"))
        }

        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
    }
    @Test
    fun `login rejects bad password`() {
        val service = authService(ArrayDeque(listOf("access-1", "refresh-1")), InMemoryTokenSessionRepository())
        val exception = assertThrows(BusinessException::class.java) {
            service.login(LoginRequest("admin", "wrong"))
        }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
    }
    @Test
    fun `disabled user cannot log in`() {
        val service = authService(ArrayDeque(listOf("access-1", "refresh-1")), InMemoryTokenSessionRepository(), enabled = false)
        val exception = assertThrows(BusinessException::class.java) {
            service.login(LoginRequest("admin", "secret"))
        }
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status)
    }
    @Test
    fun `refresh rotates both tokens and invalidates old pair`() {
        val tokens = ArrayDeque(listOf("access-1", "refresh-1", "access-2", "refresh-2"))
        val sessionRepository = InMemoryTokenSessionRepository()
        val service = authService(tokens, sessionRepository)
        val first = service.login(LoginRequest("admin", "secret"))
        val second = service.refresh(RefreshTokenRequest(first.refreshToken))
        assertEquals("access-2", second.accessToken)
        assertEquals("refresh-2", second.refreshToken)
        assertNotEquals(first.accessToken, second.accessToken)
        assertNotEquals(first.refreshToken, second.refreshToken)
        assertFalse(sessionRepository.hasAccess(first.accessToken))
        assertFalse(sessionRepository.hasRefresh(first.refreshToken))
        assertTrue(sessionRepository.hasAccess(second.accessToken))
        assertTrue(sessionRepository.hasRefresh(second.refreshToken))
    }
    @Test
    fun `logout deletes current token pair`() {
        val serviceRepository = InMemoryTokenSessionRepository()
        val service = authService(ArrayDeque(listOf("access-1", "refresh-1")), serviceRepository)
        val tokens = service.login(LoginRequest("admin", "secret"))
        service.logout(tokens.accessToken)
        assertFalse(serviceRepository.hasAccess(tokens.accessToken))
        assertFalse(serviceRepository.hasRefresh(tokens.refreshToken))
    }
    private fun authService(
        tokens: ArrayDeque<String>,
        sessionRepository: InMemoryTokenSessionRepository,
        enabled: Boolean = true,
        rateLimiter: LoginRateLimiter? = null,
    ): AuthService = AuthService(
        userAccountRepository = StaticUserAccountRepository(enabled),
        tokenSessionRepository = sessionRepository,
        tokenGenerator = UuidTokenGenerator { tokens.removeFirst() },
        passwordHasher = PlainTestPasswordHasher(),
        properties = SecurityProperties(accessTokenTtl = Duration.ofMinutes(30), refreshTokenTtl = Duration.ofDays(7)),
        clock = clock,
        loginRateLimiter = rateLimiter ?: PermissiveRateLimiter,
    )
    private inner class StaticUserAccountRepository(private val enabled: Boolean) : SecurityUserAccountRepository {
        override fun findForLogin(mode: LoginMode, principal: String): SecurityUserAccount? = SecurityUserAccount(
            id = userId,
            username = principal,
            passwordHash = "secret",
            displayName = "Administrator",
            enabled = enabled,
            roles = listOf("ADMIN"),
            permissions = listOf("project:read", "project:write"),
        )
    }
    private class PlainTestPasswordHasher : PasswordHasher {
        override fun matches(rawPassword: String, passwordHash: String): Boolean = rawPassword == passwordHash
    }
    private class InMemoryTokenSessionRepository : TokenSessionRepository {
        private val accessSessions = mutableMapOf<String, TokenSession>()
        private val refreshSessions = mutableMapOf<String, TokenSession>()
        override fun save(session: TokenSession, accessTokenTtl: Duration, refreshTokenTtl: Duration) {
            accessSessions[session.accessToken] = session
            refreshSessions[session.refreshToken] = session
        }
        override fun findByAccessToken(accessToken: String): TokenSession? = accessSessions[accessToken]
        override fun findByRefreshToken(refreshToken: String): TokenSession? = refreshSessions[refreshToken]
        override fun delete(session: TokenSession) {
            accessSessions.remove(session.accessToken)
            refreshSessions.remove(session.refreshToken)
        }
        fun findAccess(token: String): TokenSession? = accessSessions[token]
        fun findRefresh(token: String): TokenSession? = refreshSessions[token]
        fun hasAccess(token: String): Boolean = accessSessions.containsKey(token)
        fun hasRefresh(token: String): Boolean = refreshSessions.containsKey(token)
    }

    companion object {
        private val PermissiveRateLimiter = object : LoginRateLimiter(
            redisTemplate = StubStringRedisTemplate(),
            properties = SecurityProperties(loginRateLimit = SecurityProperties.LoginRateLimit(enabled = false)),
            clock = Clock.systemUTC(),
        ) {
            override fun isAllowed(principal: String): Boolean = true
            override fun recordSuccess(principal: String) = Unit
        }

        private class StubStringRedisTemplate : StringRedisTemplate() {
            init {
                // never used: permissive limiter short-circuits to true before any Redis call
            }
        }
    }
}

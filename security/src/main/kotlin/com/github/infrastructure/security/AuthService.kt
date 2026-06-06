package com.github.infrastructure.security

import com.github.infrastructure.core.web.BusinessException
import org.springframework.http.HttpStatus
import java.time.Clock
import java.time.Instant

data class LoginRequest(
    val username: String,
    val password: String,
)

data class RefreshTokenRequest(
    val refreshToken: String,
)

class AuthService(
    private val userAccountRepository: SecurityUserAccountRepository,
    private val tokenSessionRepository: TokenSessionRepository,
    private val tokenGenerator: UuidTokenGenerator,
    private val passwordHasher: PasswordHasher,
    private val properties: SecurityProperties,
    private val clock: Clock,
) {
    fun login(request: LoginRequest): TokenPair {
        val account = userAccountRepository.findByUsername(request.username) ?: throw unauthorized()
        if (!account.enabled || !passwordHasher.matches(request.password, account.passwordHash)) {
            throw unauthorized()
        }
        return createSession(account.toAuthenticatedUser())
    }

    fun refresh(request: RefreshTokenRequest): TokenPair {
        val oldSession = tokenSessionRepository.findByRefreshToken(request.refreshToken) ?: throw unauthorized()
        tokenSessionRepository.delete(oldSession)
        return createSession(oldSession.user)
    }

    fun logout(accessToken: String) {
        val session = tokenSessionRepository.findByAccessToken(accessToken) ?: return
        tokenSessionRepository.delete(session)
    }

    fun authenticate(accessToken: String): TokenSession =
        tokenSessionRepository.findByAccessToken(accessToken) ?: throw unauthorized()

    private fun createSession(user: AuthenticatedUser): TokenPair {
        val now = Instant.now(clock)
        val accessToken = tokenGenerator.next()
        val refreshToken = tokenGenerator.next()
        val session = TokenSession(
            accessToken = accessToken,
            refreshToken = refreshToken,
            user = user,
            issuedAt = now,
            accessTokenExpiresAt = now.plus(properties.accessTokenTtl),
            refreshTokenExpiresAt = now.plus(properties.refreshTokenTtl),
        )
        tokenSessionRepository.save(session, properties.accessTokenTtl, properties.refreshTokenTtl)
        return TokenPair(
            accessToken = accessToken,
            refreshToken = refreshToken,
            accessTokenExpiresInSeconds = properties.accessTokenTtl.seconds,
            refreshTokenExpiresInSeconds = properties.refreshTokenTtl.seconds,
        )
    }

    private fun SecurityUserAccount.toAuthenticatedUser(): AuthenticatedUser =
        AuthenticatedUser(
            id = id,
            username = username,
            displayName = displayName,
            roles = roles,
            permissions = permissions,
        )
}

fun unauthorized(): BusinessException = BusinessException(HttpStatus.UNAUTHORIZED.value(), "unauthorized", HttpStatus.UNAUTHORIZED)

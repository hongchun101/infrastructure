package com.github.infrastructure.security.auth

import com.github.infrastructure.core.web.exception.BusinessException
import com.github.infrastructure.security.config.SecurityProperties
import com.github.infrastructure.security.context.AuthenticatedUser
import com.github.infrastructure.security.password.PasswordHasher
import com.github.infrastructure.security.token.TokenPair
import com.github.infrastructure.security.token.TokenSession
import com.github.infrastructure.security.token.TokenSessionRepository
import com.github.infrastructure.security.token.UuidTokenGenerator
import com.github.infrastructure.security.user.SecurityUserAccount
import com.github.infrastructure.security.user.SecurityUserAccountRepository
import java.time.Clock
import java.time.Instant
import org.springframework.http.HttpStatus

enum class LoginMode {
    USERNAME,
    EMAIL,
    PHONE,
}

data class LoginRequest(
    val username: String? = null,
    val password: String,
    val mode: LoginMode = LoginMode.USERNAME,
    val principal: String? = null,
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
        val principal = request.principal ?: request.username
        if (principal.isNullOrBlank()) {
            throw unauthorized()
        }
        val account = userAccountRepository.findForLogin(request.mode, principal) ?: throw unauthorized()
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

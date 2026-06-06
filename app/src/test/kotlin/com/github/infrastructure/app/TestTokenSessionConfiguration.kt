package com.github.infrastructure.app

import com.github.infrastructure.security.TokenSession
import com.github.infrastructure.security.TokenSessionRepository
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.Duration

@TestConfiguration(proxyBeanMethods = false)
class TestTokenSessionConfiguration {
    @Bean
    @Primary
    fun testTokenSessionRepository(): TokenSessionRepository = InMemoryTokenSessionRepository()

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
    }
}

package com.github.infrastructure.security.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.security.controller.AuthController
import com.github.infrastructure.security.filter.AccessTokenAuthenticationFilter
import com.github.infrastructure.security.password.PasswordHasher
import com.github.infrastructure.security.permission.PermissionChecker
import com.github.infrastructure.security.repository.SecurityUserAccountRepository
import com.github.infrastructure.security.service.AuthService
import com.github.infrastructure.security.service.LoginRateLimiter
import com.github.infrastructure.security.token.RedisTokenSessionRepository
import com.github.infrastructure.security.token.TokenSession
import com.github.infrastructure.security.token.TokenSessionRepository
import com.github.infrastructure.security.token.UuidTokenGenerator
import jakarta.servlet.http.HttpServletResponse
import java.time.Clock
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@AutoConfiguration
@EnableMethodSecurity
@EnableConfigurationProperties(SecurityProperties::class)
class SecurityAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    fun tokenGenerator(): UuidTokenGenerator = UuidTokenGenerator()
    @Bean
    @ConditionalOnMissingBean
    fun clock(): Clock = Clock.systemUTC()
    @Bean
    @ConditionalOnMissingBean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
    @Bean
    @ConditionalOnMissingBean
    fun passwordHasher(passwordEncoder: PasswordEncoder): PasswordHasher = object : PasswordHasher {
        override fun matches(rawPassword: String, passwordHash: String): Boolean = passwordEncoder.matches(rawPassword, passwordHash)
        override fun encode(rawPassword: String): String = passwordEncoder.encode(rawPassword)
    }
    @Bean
    @ConditionalOnMissingBean
    fun tokenSessionRedisTemplate(
        redisConnectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper,
    ): RedisTemplate<String, TokenSession> {
        val template = RedisTemplate<String, TokenSession>()
        template.connectionFactory = redisConnectionFactory
        template.keySerializer = StringRedisSerializer()
        template.valueSerializer = Jackson2JsonRedisSerializer(objectMapper, TokenSession::class.java)
        template.afterPropertiesSet()
        return template
    }
    @Bean
    @ConditionalOnMissingBean
    fun tokenSessionRepository(redisTemplate: RedisTemplate<String, TokenSession>): TokenSessionRepository = RedisTokenSessionRepository(redisTemplate)
    @Bean
    @ConditionalOnMissingBean
    fun authService(
        userAccountRepository: SecurityUserAccountRepository,
        tokenSessionRepository: TokenSessionRepository,
        tokenGenerator: UuidTokenGenerator,
        passwordHasher: PasswordHasher,
        properties: SecurityProperties,
        clock: Clock,
        loginRateLimiter: LoginRateLimiter,
    ): AuthService = AuthService(
        userAccountRepository,
        tokenSessionRepository,
        tokenGenerator,
        passwordHasher,
        properties,
        clock,
        loginRateLimiter,
    )
    @Bean
    @ConditionalOnMissingBean
    fun authController(authService: AuthService): AuthController = AuthController(authService)
    @Bean
    @ConditionalOnMissingBean
    fun accessTokenAuthenticationFilter(authService: AuthService): AccessTokenAuthenticationFilter = AccessTokenAuthenticationFilter(authService)
    @Bean
    @ConditionalOnMissingBean
    fun permissionChecker(): PermissionChecker = PermissionChecker()
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        accessTokenAuthenticationFilter: AccessTokenAuthenticationFilter,
    ): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers(
                "/auth/login",
                "/auth/refresh",
                "/actuator/health",
                "/actuator/health/**",
                "/actuator/info",
                "/actuator/prometheus",
                "/actuator/metrics",
                "/actuator/metrics/**",
                "/v3/api-docs/**",
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/webjars/**",
                // Local filestore transfer: clients PUT/GET here using HMAC tokens,
                // never bearer tokens. S3-compatible providers bypass this entirely.
                "/api/files/transfer/**",
                "/error",
            ).permitAll()
            it.anyRequest().authenticated()
        }
        .exceptionHandling {
            it.authenticationEntryPoint { _, response, _ -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED) }
        }
        .addFilterBefore(accessTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()
}

package com.github.infrastructure.security

import com.fasterxml.jackson.databind.ObjectMapper
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
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.time.Clock

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
    ): AuthService = AuthService(userAccountRepository, tokenSessionRepository, tokenGenerator, passwordHasher, properties, clock)

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
            it.requestMatchers("/auth/login", "/auth/refresh", "/actuator/health", "/error").permitAll()
            it.anyRequest().authenticated()
        }
        .exceptionHandling {
            it.authenticationEntryPoint { _, response, _ -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED) }
        }
        .addFilterBefore(accessTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
        .build()
}

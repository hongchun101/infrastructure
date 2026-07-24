package com.github.infrastructure.app.audit.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.app.audit.interceptor.AuditExceptionCaptureResolver
import com.github.infrastructure.app.audit.interceptor.OperationLogInterceptor
import com.github.infrastructure.app.audit.login.LoginAuditRecorder
import com.github.infrastructure.observability.config.ObservabilityProperties
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.time.Clock

@Configuration
@EnableAsync
@EnableScheduling
class AuditAutoConfiguration(
    private val operationLogInterceptor: OperationLogInterceptor,
    private val auditExceptionCaptureResolver: AuditExceptionCaptureResolver,
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(operationLogInterceptor)
    }

    override fun extendHandlerExceptionResolvers(resolvers: MutableList<HandlerExceptionResolver>) {
        resolvers.add(0, auditExceptionCaptureResolver)
    }

    @Bean
    fun loginAuditRecorder(
        sql: KSqlClient,
        objectMapper: ObjectMapper,
        observabilityProperties: ObservabilityProperties,
        clock: Clock,
    ): LoginAuditRecorder = LoginAuditRecorder(sql, objectMapper, observabilityProperties, clock)

    @Bean
    fun loginAuditFilter(recorder: LoginAuditRecorder): FilterRegistrationBean<LoginAuditRecorder> =
        FilterRegistrationBean(recorder).apply {
            order = Ordered.HIGHEST_PRECEDENCE + 10
            addUrlPatterns("/auth/login", "/auth/logout")
            setName("loginAuditFilter")
        }
}

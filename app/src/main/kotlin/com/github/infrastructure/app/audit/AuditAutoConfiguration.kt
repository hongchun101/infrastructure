package com.github.infrastructure.app.audit

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

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
}
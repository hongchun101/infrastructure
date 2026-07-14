package com.github.infrastructure.app.audit.interceptor

import com.github.infrastructure.app.audit.annotation.OperationLog
import com.github.infrastructure.app.audit.event.OperationLogEvent
import com.github.infrastructure.observability.config.ObservabilityProperties
import com.github.infrastructure.security.context.CurrentUserContext
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Component
import org.springframework.web.method.HandlerMethod
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping

@Component
class OperationLogInterceptor(
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val observabilityProperties: ObservabilityProperties,
    private val clock: Clock,
) : HandlerInterceptor {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (handler !is HandlerMethod) return true
        if (handler.getMethodAnnotation(OperationLog::class.java) == null) return true
        request.setAttribute(START_TIME_ATTR, System.currentTimeMillis())
        return true
    }

    override fun afterCompletion(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        ex: Exception?,
    ) {
        if (handler !is HandlerMethod) return
        val annotation = handler.getMethodAnnotation(OperationLog::class.java) ?: return
        try {
            val startTime = request.getAttribute(START_TIME_ATTR) as? Long ?: return
            val now = LocalDateTime.now(clock)
            val durationMs = (System.currentTimeMillis() - startTime).coerceAtLeast(0)
            val path = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE) as? String
                ?: request.requestURI
            val user = CurrentUserContext.get()
            val event = OperationLogEvent(
                id = UUID.randomUUID(),
                traceId = MDC.get(observabilityProperties.mdcKey),
                userId = user?.id,
                username = user?.username,
                module = annotation.module,
                action = annotation.action,
                description = annotation.description.takeIf { it.isNotBlank() },
                method = request.method,
                path = path,
                queryString = request.queryString?.take(2000),
                responseStatus = response.status,
                errorMessage = (ex?.message
                    ?: request.getAttribute(AuditExceptionCaptureResolver.AUDIT_ERROR_MESSAGE_ATTR) as? String
                    ?: errorMessageAttribute(request))?.take(2000),
                clientIp = clientIp(request),
                userAgent = request.getHeader("User-Agent")?.take(500),
                durationMs = durationMs,
                success = ex == null && response.status < 400,
                createdTime = now,
            )
            applicationEventPublisher.publishEvent(event)
        } catch (e: Exception) {
            log.warn("failed to publish operation log event for {} {}", request.method, request.requestURI, e)
        }
    }

    private fun clientIp(request: HttpServletRequest): String? {
        val forwarded = request.getHeader("X-Forwarded-For")?.substringBefore(',')?.trim()
        if (!forwarded.isNullOrBlank()) return forwarded.take(64)
        val real = request.getHeader("X-Real-IP")?.trim()
        if (!real.isNullOrBlank()) return real.take(64)
        return request.remoteAddr?.take(64)
    }

    private fun errorMessageAttribute(request: HttpServletRequest): String? {
        val attr = request.getAttribute("jakarta.servlet.error.message") as? String
        return attr?.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val START_TIME_ATTR = "audit.startTime"
    }
}

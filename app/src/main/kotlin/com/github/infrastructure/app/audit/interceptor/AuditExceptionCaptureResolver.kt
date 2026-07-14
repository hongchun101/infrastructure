package com.github.infrastructure.app.audit.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerExceptionResolver
import org.springframework.web.servlet.ModelAndView

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class AuditExceptionCaptureResolver : HandlerExceptionResolver {
    override fun resolveException(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        ex: Exception,
    ): ModelAndView? {
        val message = ex.message ?: ex::class.simpleName
        if (!message.isNullOrBlank()) {
            request.setAttribute(AUDIT_ERROR_MESSAGE_ATTR, message)
        }
        return null
    }

    companion object {
        const val AUDIT_ERROR_MESSAGE_ATTR = "audit.errorMessage"
    }
}

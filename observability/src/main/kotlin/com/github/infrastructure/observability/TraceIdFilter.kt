package com.github.infrastructure.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.web.filter.OncePerRequestFilter
import java.util.UUID

class TraceIdFilter(
    private val properties: ObservabilityProperties,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val traceId = request.getHeader(properties.traceIdHeader)?.takeIf(String::isNotBlank) ?: UUID.randomUUID().toString()
        response.setHeader(properties.traceIdHeader, traceId)
        MDC.put(properties.mdcKey, traceId)
        try {
            filterChain.doFilter(request, response)
        } finally {
            MDC.remove(properties.mdcKey)
        }
    }
}

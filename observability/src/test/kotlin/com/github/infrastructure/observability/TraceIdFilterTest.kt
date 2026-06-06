package com.github.infrastructure.observability

import jakarta.servlet.FilterChain
import jakarta.servlet.ServletRequest
import jakarta.servlet.ServletResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

class TraceIdFilterTest {
    @Test
    fun `uses incoming trace id during request and writes response header`() {
        val properties = ObservabilityProperties()
        val filter = TraceIdFilter(properties)
        val request = MockHttpServletRequest().apply {
            addHeader("X-Trace-Id", "trace-123")
        }
        val response = MockHttpServletResponse()
        var traceIdDuringChain: String? = null

        filter.doFilter(request, response, FilterChain { _: ServletRequest, _: ServletResponse ->
            traceIdDuringChain = MDC.get("traceId")
        })

        assertThat(traceIdDuringChain).isEqualTo("trace-123")
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo("trace-123")
        assertThat(MDC.get("traceId")).isNull()
    }

    @Test
    fun `generates trace id when request does not provide one`() {
        val properties = ObservabilityProperties()
        val filter = TraceIdFilter(properties)
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        var traceIdDuringChain: String? = null

        filter.doFilter(request, response, FilterChain { _: ServletRequest, _: ServletResponse ->
            traceIdDuringChain = MDC.get("traceId")
        })

        assertThat(traceIdDuringChain).isNotBlank()
        assertThat(response.getHeader("X-Trace-Id")).isEqualTo(traceIdDuringChain)
        assertThat(MDC.get("traceId")).isNull()
    }
}

package com.github.infrastructure.app.audit.login

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.infrastructure.app.audit.login.repository.LoginAuditEntry
import com.github.infrastructure.app.audit.login.repository.LoginOutcome
import com.github.infrastructure.observability.config.ObservabilityProperties
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.springframework.http.HttpMethod
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.nio.charset.StandardCharsets
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * 登录/登出审计过滤器：拦截 `/auth/login` 与 `/auth/logout` 请求，将结果（成功/失败/登出、客户端 IP、UA、traceId、principal）写入 `login_audits` 表。
 *
 * 之所以使用过滤器而非在 `AuthService` 中发布事件，是为了避免 `app` 模块对 `security` 模块的逆向依赖。过滤器仅在 `app` 自身注册，
 * 借助 `OncePerRequestFilter` + Spring 的 `ContentCachingRequestWrapper`/`ContentCachingResponseWrapper` 包装请求/响应，
 * 在控制器执行后读取请求体/响应体判断成功或失败原因。包装器对原始流透明——它不消费流，只在 `getInputStream()` 被调用时缓存已读取的字节，
 * 因此对 Spring MVC 的 JSON 反序列化路径完全无副作用。
 */
class LoginAuditRecorder(
    private val sql: KSqlClient,
    private val objectMapper: ObjectMapper,
    private val observabilityProperties: ObservabilityProperties,
    private val clock: Clock,
) : OncePerRequestFilter() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun shouldNotFilter(request: HttpServletRequest): Boolean {
        val path = request.requestURI
        return !(HttpMethod.POST.matches(request.method) &&
            (path == "/auth/login" || path == "/auth/logout"))
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val path = request.requestURI
        val isLogin = path == "/auth/login"
        // ContentCachingRequestWrapper 只在调用 getInputStream()/getReader() 之后才缓存内容，
        // 因此包装请求不会影响 Spring MVC 读取请求体。
        val cachedRequest = if (isLogin) ContentCachingRequestWrapper(request) else request
        val cachedResponse = ContentCachingResponseWrapper(response)

        filterChain.doFilter(cachedRequest, cachedResponse)

        try {
            val auditOutcome = when {
                path == "/auth/logout" -> LoginOutcome.LOGOUT
                cachedResponse.status in 200..299 -> LoginOutcome.SUCCESS
                else -> LoginOutcome.FAILURE
            }
            val requestBytes: ByteArray = if (isLogin) (cachedRequest as ContentCachingRequestWrapper).contentAsByteArray else ByteArray(0)
            val requestJson = if (isLogin) parseBody(requestBytes) else null
            val responseJson = parseBody(cachedResponse.contentAsByteArray)
            val now = LocalDateTime.now(clock)

            sql.save(
                LoginAuditEntry {
                    id = UUID.randomUUID()
                    accountType = (requestJson?.get("accountType")?.asText()
                        ?: responseJson?.get("data")?.get("accountType")?.asText()
                        ?: "USER")
                    loginMode = requestJson?.get("mode")?.asText()
                    principal = requestJson?.get("principal")?.asText()
                        ?: requestJson?.get("username")?.asText()
                        ?: requestJson?.get("email")?.asText()
                        ?: requestJson?.get("phone")?.asText()
                    accountId = responseJson?.get("data")?.get("userId")?.asText()
                        ?.let { runCatching { UUID.fromString(it) }.getOrNull() }
                    username = responseJson?.get("data")?.get("username")?.asText()
                        ?: requestJson?.get("username")?.asText()
                    outcome = auditOutcome.name
                    failureReason = (if (auditOutcome == LoginOutcome.FAILURE) {
                        responseJson?.get("message")?.asText() ?: "login failed"
                    } else null)?.take(500)
                    clientIp = clientIp(request)
                    userAgent = request.getHeader("User-Agent")?.take(500)
                    traceId = MDC.get(observabilityProperties.mdcKey)
                    createdTime = now
                },
            )
        } catch (e: Exception) {
            log.warn("failed to record login audit for path={}", path, e)
        } finally {
            // ContentCachingResponseWrapper 缓冲了响应体；必须显式写回实际响应，否则客户端收到 0 字节。
            cachedResponse.copyBodyToResponse()
        }
    }

    private fun parseBody(bytes: ByteArray): JsonNode? = try {
        if (bytes.isEmpty()) null else objectMapper.readTree(bytes)
    } catch (e: Exception) {
        null
    }

    private fun clientIp(request: HttpServletRequest): String? {
        val forwarded = request.getHeader("X-Forwarded-For")?.substringBefore(',')?.trim()
        if (!forwarded.isNullOrBlank()) return forwarded.take(64)
        val real = request.getHeader("X-Real-IP")?.trim()
        if (!real.isNullOrBlank()) return real.take(64)
        return request.remoteAddr?.take(64)
    }
}

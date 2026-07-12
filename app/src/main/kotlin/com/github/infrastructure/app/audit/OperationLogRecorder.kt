package com.github.infrastructure.app.audit

import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class OperationLogRecorder(
    private val jdbcClient: JdbcClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun onEvent(event: OperationLogEvent) {
        try {
            jdbcClient.sql(
                """
                insert into operation_logs
                    (id, trace_id, user_id, username, module, action, description,
                     method, path, query_string, response_status, error_message,
                     client_ip, user_agent, duration_ms, success, created_time)
                values
                    (:id, :traceId, :userId, :username, :module, :action, :description,
                     :method, :path, :queryString, :responseStatus, :errorMessage,
                     :clientIp, :userAgent, :durationMs, :success, :createdTime)
                """.trimIndent(),
            )
                .param("id", event.id)
                .param("traceId", event.traceId)
                .param("userId", event.userId)
                .param("username", event.username)
                .param("module", event.module)
                .param("action", event.action)
                .param("description", event.description)
                .param("method", event.method)
                .param("path", event.path)
                .param("queryString", event.queryString)
                .param("responseStatus", event.responseStatus)
                .param("errorMessage", event.errorMessage)
                .param("clientIp", event.clientIp)
                .param("userAgent", event.userAgent)
                .param("durationMs", event.durationMs)
                .param("success", event.success)
                .param("createdTime", event.createdTime)
                .update()
        } catch (e: Exception) {
            log.error(
                "failed to persist operation log id={} module={} action={}",
                event.id,
                event.module,
                event.action,
                e,
            )
        }
    }
}
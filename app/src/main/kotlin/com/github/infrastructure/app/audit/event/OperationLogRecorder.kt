package com.github.infrastructure.app.audit.event

import com.github.infrastructure.app.audit.entity.OperationLogEntry
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class OperationLogRecorder(
    private val sql: KSqlClient,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun onEvent(event: OperationLogEvent) {
        try {
            sql.save(
                OperationLogEntry {
                    id = event.id
                    traceId = event.traceId
                    userId = event.userId
                    username = event.username
                    module = event.module
                    action = event.action
                    description = event.description
                    method = event.method
                    path = event.path
                    queryString = event.queryString
                    responseStatus = event.responseStatus
                    errorMessage = event.errorMessage
                    clientIp = event.clientIp
                    userAgent = event.userAgent
                    durationMs = event.durationMs
                    success = event.success
                    createdTime = event.createdTime
                },
            )
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

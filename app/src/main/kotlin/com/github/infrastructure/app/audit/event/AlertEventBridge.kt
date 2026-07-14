package com.github.infrastructure.app.audit.event

import com.github.infrastructure.alert.rule.OperationLogSignal
import com.github.infrastructure.alert.service.AlertEvaluationService
import org.slf4j.LoggerFactory
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

/**
 * Bridge between the audit module and the alert module. It listens for every
 * [OperationLogEvent] (the same event persisted by [OperationLogRecorder]) and
 * hands it off to the alert engine for evaluation as a platform-neutral
 * [OperationLogSignal].
 */
@Component
class AlertEventBridge(
    private val alertEvaluationService: AlertEvaluationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Async
    @EventListener
    fun onOperationLog(event: OperationLogEvent) {
        try {
            alertEvaluationService.evaluate(event.toSignal())
        } catch (e: Exception) {
            log.warn("alert evaluation failed for module={} action={}", event.module, event.action, e)
        }
    }

    private fun OperationLogEvent.toSignal(): OperationLogSignal = OperationLogSignal(
        id = id,
        traceId = traceId,
        userId = userId,
        username = username,
        module = module,
        action = action,
        description = description,
        method = method,
        path = path,
        queryString = queryString,
        responseStatus = responseStatus,
        errorMessage = errorMessage,
        clientIp = clientIp,
        userAgent = userAgent,
        durationMs = durationMs,
        success = success,
    )
}

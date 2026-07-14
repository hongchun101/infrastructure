package com.github.infrastructure.export.handler

import kotlin.reflect.KClass
import org.springframework.stereotype.Component

/**
 * Discovers every [ExportHandler] bean at startup and indexes them by
 * [ExportHandler.type]. The runner consults this registry both to dispatch
 * a queued job and to bootstrap the user-facing `/export-jobs/types` endpoint.
 */
@Component
class ExportHandlerRegistry(
    handlers: List<ExportHandler<*, *>>,
) {
    private val byType: Map<String, ExportHandler<*, *>> =
        handlers.associateBy { it.type }

    fun all(): List<ExportHandler<*, *>> = byType.values.sortedBy { it.type }

    fun get(type: String): ExportHandler<*, *>? = byType[type]

    fun types(): Set<String> = byType.keys

    @Suppress("UNCHECKED_CAST")
    fun <P : Any, R : Any> cast(handler: ExportHandler<*, *>, paramClass: KClass<P>): ExportHandler<P, R>? {
        if (handler.parameterClass() != paramClass) return null
        return handler as ExportHandler<P, R>
    }
}
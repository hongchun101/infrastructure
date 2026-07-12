package com.github.infrastructure.app.audit

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class OperationLog(
    val module: String,
    val action: String,
    val description: String = "",
)
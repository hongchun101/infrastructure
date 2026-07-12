package com.github.infrastructure.app.audit

data class PageResponse<T>(
    val items: List<T>,
    val total: Long,
    val page: Int,
    val size: Int,
)
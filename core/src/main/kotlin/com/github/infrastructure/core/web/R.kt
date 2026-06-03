package com.github.infrastructure.core.web

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class R<out T>(
    val code: Int,
    val message: String,
    val data: T?,
) {
    companion object {
        private const val SUCCESS_CODE = 0
        private const val SUCCESS_MESSAGE = "success"

        fun <T> ok(data: T?): R<T> = R(SUCCESS_CODE, SUCCESS_MESSAGE, data)
    }
}

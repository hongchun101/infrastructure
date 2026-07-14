package com.github.infrastructure.scheduler.error

import com.github.infrastructure.core.web.exception.BusinessException
import org.springframework.http.HttpStatus

object JobErrors {
    fun notFound(code: String): BusinessException =
        BusinessException(40400, "job '$code' not found", HttpStatus.NOT_FOUND)
    fun disabled(code: String): BusinessException =
        BusinessException(40900, "job '$code' is disabled; resume it before triggering", HttpStatus.CONFLICT)
}
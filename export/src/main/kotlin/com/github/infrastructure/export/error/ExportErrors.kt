package com.github.infrastructure.export.error

import com.github.infrastructure.core.web.exception.BusinessException
import org.springframework.http.HttpStatus

object ExportErrors {
    fun notFound(id: Long): BusinessException =
        BusinessException(40400, "export job $id not found", HttpStatus.NOT_FOUND)
    fun notFoundType(businessType: String): BusinessException =
        BusinessException(40401, "no export handler for businessType '$businessType'", HttpStatus.NOT_FOUND)
    fun illegalState(message: String): BusinessException =
        BusinessException(40900, message, HttpStatus.CONFLICT)
    fun alreadyTerminal(id: Long, status: String): BusinessException =
        BusinessException(40901, "export job $id is in terminal status '$status'", HttpStatus.CONFLICT)
    fun tooManyConcurrent(owner: String): BusinessException =
        BusinessException(42900, "too many concurrent export jobs for $owner", HttpStatus.TOO_MANY_REQUESTS)
    fun badParams(message: String): BusinessException =
        BusinessException(40000, "invalid export parameters: $message", HttpStatus.BAD_REQUEST)
}
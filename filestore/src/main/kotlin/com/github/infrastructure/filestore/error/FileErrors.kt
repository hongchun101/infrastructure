package com.github.infrastructure.filestore.error

import com.github.infrastructure.core.web.exception.BusinessException
import org.springframework.http.HttpStatus

object FileErrors {
    fun notFound(id: Long): BusinessException =
        BusinessException(40400, "file $id not found", HttpStatus.NOT_FOUND)
    fun notPending(id: Long, status: String): BusinessException =
        BusinessException(40900, "file $id is in status $status, expected PENDING", HttpStatus.CONFLICT)
    fun alreadyUploaded(id: Long): BusinessException =
        BusinessException(40901, "file $id already uploaded", HttpStatus.CONFLICT)
    fun uploadMismatch(id: Long, reason: String): BusinessException =
        BusinessException(40902, "upload verification failed for $id: $reason", HttpStatus.CONFLICT)
    fun invalidVisibility(v: String): BusinessException =
        BusinessException(40000, "invalid visibility '$v'", HttpStatus.BAD_REQUEST)
    fun tooLarge(allowed: Long, got: Long): BusinessException =
        BusinessException(41300, "file too large: $got > $allowed", HttpStatus.PAYLOAD_TOO_LARGE)
    fun contentTypeDenied(t: String): BusinessException =
        BusinessException(41500, "content type '$t' is not allowed", HttpStatus.UNSUPPORTED_MEDIA_TYPE)
    fun notOwner(): BusinessException =
        BusinessException(40300, "caller is not the owner of this file", HttpStatus.FORBIDDEN)
    fun transferForbidden(reason: String): BusinessException =
        BusinessException(40301, "transfer token rejected: $reason", HttpStatus.FORBIDDEN)
}
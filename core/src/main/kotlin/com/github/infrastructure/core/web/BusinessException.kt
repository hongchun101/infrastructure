package com.github.infrastructure.core.web

import org.springframework.http.HttpStatus

open class BusinessException(
    val code: Int,
    override val message: String,
    val status: HttpStatus,
) : RuntimeException(message)

class BadRequestException(
    message: String,
    code: Int = HttpStatus.BAD_REQUEST.value(),
) : BusinessException(code, message, HttpStatus.BAD_REQUEST)

class UnauthorizedException(
    message: String,
    code: Int = HttpStatus.UNAUTHORIZED.value(),
) : BusinessException(code, message, HttpStatus.UNAUTHORIZED)

class ForbiddenException(
    message: String,
    code: Int = HttpStatus.FORBIDDEN.value(),
) : BusinessException(code, message, HttpStatus.FORBIDDEN)

class NotFoundException(
    message: String,
    code: Int = HttpStatus.NOT_FOUND.value(),
) : BusinessException(code, message, HttpStatus.NOT_FOUND)

class ConflictException(
    message: String,
    code: Int = HttpStatus.CONFLICT.value(),
) : BusinessException(code, message, HttpStatus.CONFLICT)

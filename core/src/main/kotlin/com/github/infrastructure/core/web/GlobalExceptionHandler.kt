package com.github.infrastructure.core.web

import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.NoHandlerFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<R<Nothing>> =
        error(exception.status, exception.code, exception.message)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(exception: MethodArgumentNotValidException): ResponseEntity<R<Nothing>> =
        error(HttpStatus.BAD_REQUEST, validationMessage(exception))

    @ExceptionHandler(BindException::class)
    fun handleBindException(exception: BindException): ResponseEntity<R<Nothing>> =
        error(HttpStatus.BAD_REQUEST, validationMessage(exception))

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(exception: ConstraintViolationException): ResponseEntity<R<Nothing>> =
        error(HttpStatus.BAD_REQUEST, constraintViolationMessage(exception))

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(exception: HttpMessageNotReadableException): ResponseEntity<R<Nothing>> =
        error(HttpStatus.BAD_REQUEST, "malformed request body")

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameterException(
        exception: MissingServletRequestParameterException,
    ): ResponseEntity<R<Nothing>> =
        error(HttpStatus.BAD_REQUEST, "missing required parameter: ${exception.parameterName}")

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatchException(
        exception: MethodArgumentTypeMismatchException,
    ): ResponseEntity<R<Nothing>> =
        error(HttpStatus.BAD_REQUEST, "invalid value for parameter: ${exception.name}")

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFoundException(exception: NoHandlerFoundException): ResponseEntity<R<Nothing>> =
        error(HttpStatus.NOT_FOUND, "resource not found")

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupportedException(
        exception: HttpRequestMethodNotSupportedException,
    ): ResponseEntity<R<Nothing>> =
        error(HttpStatus.METHOD_NOT_ALLOWED, "request method not supported")

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleHttpMediaTypeNotSupportedException(
        exception: HttpMediaTypeNotSupportedException,
    ): ResponseEntity<R<Nothing>> =
        error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "media type not supported")

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(exception: ResponseStatusException): ResponseEntity<R<Nothing>> =
        error(HttpStatus.valueOf(exception.statusCode.value()), exception.reason ?: "request failed")

    @ExceptionHandler(Exception::class)
    fun handleException(exception: Exception): ResponseEntity<R<Nothing>> =
        error(HttpStatus.INTERNAL_SERVER_ERROR, "internal server error")

    private fun error(status: HttpStatus, message: String): ResponseEntity<R<Nothing>> =
        error(status, status.value(), message)

    private fun error(status: HttpStatus, code: Int, message: String): ResponseEntity<R<Nothing>> =
        ResponseEntity.status(status).body(R.error(code, message))

    private fun validationMessage(exception: BindException): String {
        val fieldError = exception.bindingResult.fieldError ?: return "bad request"
        val defaultMessage = fieldError.defaultMessage ?: return "bad request"
        return "${fieldError.field} $defaultMessage"
    }

    private fun constraintViolationMessage(exception: ConstraintViolationException): String {
        val violation = exception.constraintViolations.firstOrNull() ?: return "bad request"
        val property = violation.propertyPath?.toString()?.substringAfterLast('.')
        val message = violation.message
        return if (property.isNullOrBlank()) message else "$property $message"
    }
}

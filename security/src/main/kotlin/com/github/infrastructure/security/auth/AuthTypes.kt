package com.github.infrastructure.security.auth

import com.github.infrastructure.core.web.exception.BusinessException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus

enum class AccountType {
    USER,
    BACKEND,
}
enum class LoginMode {
    USERNAME,
    EMAIL,
    PHONE,
}

data class LoginRequest(
    val username: String? = null,
    val password: String,
    val mode: LoginMode = LoginMode.USERNAME,
    val principal: String? = null,
    val accountType: AccountType = AccountType.USER,
)

data class RefreshTokenRequest(
    val refreshToken: String,
)

fun unauthorized(): BusinessException =
    BusinessException(HttpStatus.UNAUTHORIZED.value(), "unauthorized", HttpStatus.UNAUTHORIZED)

fun bearerToken(request: HttpServletRequest): String? {
    val header = request.getHeader("Authorization") ?: return null
    if (!header.startsWith("Bearer ")) {
        return null
    }
    return header.removePrefix("Bearer ").takeIf { it.isNotBlank() }
}

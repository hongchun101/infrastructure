package com.github.infrastructure.security.auth

import com.github.infrastructure.security.context.AuthenticatedUser
import com.github.infrastructure.security.context.CurrentUserContext
import com.github.infrastructure.security.token.TokenPair
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AuthController(
    private val authService: AuthService,
) {
    @PostMapping("/auth/login")
    fun login(@RequestBody request: LoginRequest): TokenPair = authService.login(request)
    @PostMapping("/auth/refresh")
    fun refresh(@RequestBody request: RefreshTokenRequest): TokenPair = authService.refresh(request)
    @PostMapping("/auth/logout")
    fun logout(request: HttpServletRequest) {
        authService.logout(bearerToken(request) ?: throw unauthorized())
    }
    @GetMapping("/me")
    fun me(): AuthenticatedUser = CurrentUserContext.require()
}
fun bearerToken(request: HttpServletRequest): String? {
    val header = request.getHeader("Authorization") ?: return null
    if (!header.startsWith("Bearer ")) {
        return null
    }
    return header.removePrefix("Bearer ").takeIf { it.isNotBlank() }
}

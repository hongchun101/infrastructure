package com.github.infrastructure.security.controller

import com.github.infrastructure.security.auth.LoginRequest
import com.github.infrastructure.security.auth.RefreshTokenRequest
import com.github.infrastructure.security.auth.bearerToken
import com.github.infrastructure.security.auth.unauthorized
import com.github.infrastructure.security.context.AuthenticatedUser
import com.github.infrastructure.security.context.CurrentUserContext
import com.github.infrastructure.security.service.AuthService
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

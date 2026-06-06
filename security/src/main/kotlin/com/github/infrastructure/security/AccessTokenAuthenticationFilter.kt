package com.github.infrastructure.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter

class AccessTokenAuthenticationFilter(
    private val authService: AuthService,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val token = bearerToken(request)
        if (token != null) {
            val session = authService.authenticate(token)
            val authorities = session.user.permissions.map(::SimpleGrantedAuthority)
            SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(session.user, token, authorities)
        }
        filterChain.doFilter(request, response)
    }
}

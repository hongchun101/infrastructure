package com.github.infrastructure.security.context

import com.github.infrastructure.security.auth.unauthorized
import org.springframework.security.core.context.SecurityContextHolder

object CurrentUserContext {
    fun get(): AuthenticatedUser? =
        SecurityContextHolder.getContext().authentication?.principal as? AuthenticatedUser
    fun require(): AuthenticatedUser = get() ?: throw unauthorized()
}

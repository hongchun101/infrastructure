package com.github.infrastructure.security

class PermissionChecker {
    fun has(permission: String): Boolean = CurrentUserContext.get()?.permissions?.contains(permission) == true
}

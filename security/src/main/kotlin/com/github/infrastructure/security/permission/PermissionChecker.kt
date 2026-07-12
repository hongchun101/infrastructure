package com.github.infrastructure.security.permission

import com.github.infrastructure.security.context.CurrentUserContext

class PermissionChecker {
    fun has(permission: String): Boolean = CurrentUserContext.get()?.permissions?.contains(permission) == true
}

package com.github.infrastructure.security.user

import com.github.infrastructure.security.auth.LoginMode

interface SecurityUserAccountRepository {
    fun findForLogin(mode: LoginMode, principal: String): SecurityUserAccount?
    fun findByUsername(username: String): SecurityUserAccount? = findForLogin(LoginMode.USERNAME, username)
}

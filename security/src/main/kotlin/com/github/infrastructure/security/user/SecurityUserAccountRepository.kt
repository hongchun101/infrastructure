package com.github.infrastructure.security.user

import com.github.infrastructure.security.auth.AccountType
import com.github.infrastructure.security.auth.LoginMode

interface SecurityUserAccountRepository {
    fun findForLogin(accountType: AccountType, mode: LoginMode, principal: String): SecurityUserAccount? =
        if (accountType == AccountType.USER) findForLogin(mode, principal) else null
    fun findForLogin(mode: LoginMode, principal: String): SecurityUserAccount?
    fun findByUsername(username: String): SecurityUserAccount? = findForLogin(LoginMode.USERNAME, username)
}

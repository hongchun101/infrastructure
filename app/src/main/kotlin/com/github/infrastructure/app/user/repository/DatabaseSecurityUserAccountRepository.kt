package com.github.infrastructure.app.user.repository

import com.github.infrastructure.security.auth.AccountType
import com.github.infrastructure.security.auth.LoginMode
import com.github.infrastructure.security.user.SecurityUserAccount
import com.github.infrastructure.security.repository.SecurityUserAccountRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Repository
class DatabaseSecurityUserAccountRepository(
    private val userRepository: UserRepository,
    private val backendAccountRepository: BackendAccountRepository,
    private val userRoleRepository: UserRoleRepository,
    private val backendAccountRoleRepository: BackendAccountRoleRepository,
    private val rolePermissionRepository: RolePermissionRepository,
) : SecurityUserAccountRepository {
    @Transactional(readOnly = true)
    override fun findForLogin(mode: LoginMode, principal: String): SecurityUserAccount? =
        findForLogin(AccountType.USER, mode, principal)

    @Transactional(readOnly = true)
    override fun findForLogin(accountType: AccountType, mode: LoginMode, principal: String): SecurityUserAccount? =
        when (accountType) {
            AccountType.USER -> userRepository.findForLogin(mode, principal)?.let { user ->
                val roles = userRoleRepository.findRoleCodesByUserId(user.id)
                toSecurityAccount(user.id, user.username, user.passwordHash, user.displayName, user.enabled, roles, AccountType.USER)
            }
            AccountType.BACKEND -> backendAccountRepository.findForLogin(mode, principal)?.let { account ->
                val roles = backendAccountRoleRepository.findRoleCodesByAccountId(account.id)
                toSecurityAccount(account.id, account.username, account.passwordHash, account.displayName, account.enabled, roles, AccountType.BACKEND)
            }
        }

    private fun toSecurityAccount(
        id: UUID,
        username: String,
        passwordHash: String,
        displayName: String,
        enabled: Boolean,
        roles: List<String>,
        accountType: AccountType,
    ): SecurityUserAccount {
        val permissions = if (roles.isEmpty()) emptyList() else rolePermissionRepository.findPermissionCodesByRoleCodes(roles)
        return SecurityUserAccount(id, username, passwordHash, displayName, enabled, roles, permissions, accountType)
    }
}

package com.github.infrastructure.app.user

import com.github.infrastructure.security.auth.LoginMode
import com.github.infrastructure.security.user.SecurityUserAccount
import com.github.infrastructure.security.user.SecurityUserAccountRepository
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional

@Repository
class DatabaseSecurityUserAccountRepository(
    private val userRepository: UserRepository,
    private val userRoleRepository: UserRoleRepository,
    private val rolePermissionRepository: RolePermissionRepository,
) : SecurityUserAccountRepository {
    @Transactional(readOnly = true)
    override fun findForLogin(mode: LoginMode, principal: String): SecurityUserAccount? {
        val user = when (mode) {
            LoginMode.USERNAME -> userRepository.findByUsername(principal)
            LoginMode.EMAIL -> userRepository.findByEmail(principal)
            LoginMode.PHONE -> userRepository.findByPhone(principal)
        } ?: return null
        return toSecurityAccount(user)
    }

    private fun toSecurityAccount(user: User): SecurityUserAccount {
        val roles = userRoleRepository.findRoleCodesByUserId(user.id)
        val permissions = if (roles.isEmpty()) {
            emptyList()
        } else {
            rolePermissionRepository.findPermissionCodesByRoleCodes(roles)
        }
        return SecurityUserAccount(
            id = user.id,
            username = user.username,
            passwordHash = user.passwordHash,
            displayName = user.displayName,
            enabled = user.enabled,
            roles = roles,
            permissions = permissions,
        )
    }
}
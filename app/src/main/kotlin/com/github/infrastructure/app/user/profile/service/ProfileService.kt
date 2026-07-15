package com.github.infrastructure.app.user.profile.service

import com.github.infrastructure.app.user.entity.BackendAccount
import com.github.infrastructure.app.user.entity.User
import com.github.infrastructure.app.user.profile.ChangePasswordRequest
import com.github.infrastructure.app.user.profile.ProfileResponse
import com.github.infrastructure.app.user.profile.UpdateProfileRequest
import com.github.infrastructure.app.user.repository.BackendAccountRepository
import com.github.infrastructure.app.user.repository.UserRepository
import com.github.infrastructure.core.web.exception.BusinessException
import com.github.infrastructure.security.auth.AccountType
import com.github.infrastructure.security.context.AuthenticatedUser
import com.github.infrastructure.security.password.PasswordHasher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

/**
 * 自助资料服务：当前登录用户（USER / BACKEND）更新自己的展示信息与密码。
 *
 * 设计要点：
 * - 邮箱/手机号唯一性需要跨账号类型校验：同一邮箱不能被两个不同的账号绑定（无论是 USER 还是 BACKEND），
 *   否则会破坏登录按 principal 查找的行为。
 * - 改密必须校验旧密码，避免被劫持的 token 静默改密。
 * - 返回的是最新资料视图，便于前端在改密/改资料成功后立即刷新 `/me`。
 */
@Service
class ProfileService(
    private val userRepository: UserRepository,
    private val backendAccountRepository: BackendAccountRepository,
    private val passwordHasher: PasswordHasher,
    private val clock: Clock,
) {
    @Transactional
    fun updateProfile(request: UpdateProfileRequest, current: AuthenticatedUser): ProfileResponse =
        when (current.accountType) {
            AccountType.USER -> updateUserProfile(request, current)
            AccountType.BACKEND -> updateBackendProfile(request, current)
        }

    @Transactional
    fun changePassword(request: ChangePasswordRequest, current: AuthenticatedUser) {
        when (current.accountType) {
            AccountType.USER -> changeUserPassword(request, current.id)
            AccountType.BACKEND -> changeBackendPassword(request, current.id)
        }
    }

    private fun updateUserProfile(request: UpdateProfileRequest, current: AuthenticatedUser): ProfileResponse {
        val user = userRepository.findById(current.id) ?: throw notFound("user")
        request.email?.takeIf { it.isNotBlank() }?.let { ensureEmailAvailable(it, current.id, AccountType.USER) }
        request.phone?.takeIf { it.isNotBlank() }?.let { ensurePhoneAvailable(it, current.id, AccountType.USER) }

        val updated = userRepository.save(
            User {
                this.id = user.id
                this.username = user.username
                this.email = request.email?.takeIf { it.isNotBlank() } ?: user.email
                this.phone = request.phone?.takeIf { it.isNotBlank() } ?: user.phone
                this.passwordHash = user.passwordHash
                this.displayName = request.displayName?.takeIf { it.isNotBlank() } ?: user.displayName
                this.enabled = user.enabled
                this.createdTime = user.createdTime
            },
        ).modifiedEntity
        return updated.toResponse(current.accountType)
    }

    private fun updateBackendProfile(request: UpdateProfileRequest, current: AuthenticatedUser): ProfileResponse {
        val account = backendAccountRepository.findById(current.id) ?: throw notFound("backend account")
        request.email?.takeIf { it.isNotBlank() }?.let { ensureEmailAvailable(it, current.id, AccountType.BACKEND) }
        request.phone?.takeIf { it.isNotBlank() }?.let { ensurePhoneAvailable(it, current.id, AccountType.BACKEND) }

        val now = LocalDateTime.now(clock)
        val updated = backendAccountRepository.save(
            BackendAccount {
                this.id = account.id
                this.username = account.username
                this.email = request.email?.takeIf { it.isNotBlank() } ?: account.email
                this.phone = request.phone?.takeIf { it.isNotBlank() } ?: account.phone
                this.passwordHash = account.passwordHash
                this.displayName = request.displayName?.takeIf { it.isNotBlank() } ?: account.displayName
                this.enabled = account.enabled
                this.createdTime = account.createdTime
                this.updatedTime = now
            },
        ).modifiedEntity
        return updated.toResponse(current.accountType)
    }

    private fun changeUserPassword(request: ChangePasswordRequest, userId: UUID) {
        val user = userRepository.findById(userId) ?: throw notFound("user")
        if (!passwordHasher.matches(request.currentPassword, user.passwordHash)) {
            throw BusinessException(HttpStatus.BAD_REQUEST.value(), "current password is incorrect", HttpStatus.BAD_REQUEST)
        }
        if (request.currentPassword == request.newPassword) {
            throw BusinessException(HttpStatus.BAD_REQUEST.value(), "new password must differ from current", HttpStatus.BAD_REQUEST)
        }
        userRepository.save(
            User {
                this.id = user.id
                this.username = user.username
                this.email = user.email
                this.phone = user.phone
                this.passwordHash = passwordHasher.encode(request.newPassword)
                this.displayName = user.displayName
                this.enabled = user.enabled
                this.createdTime = user.createdTime
            },
        )
    }

    private fun changeBackendPassword(request: ChangePasswordRequest, accountId: UUID) {
        val account = backendAccountRepository.findById(accountId) ?: throw notFound("backend account")
        if (!passwordHasher.matches(request.currentPassword, account.passwordHash)) {
            throw BusinessException(HttpStatus.BAD_REQUEST.value(), "current password is incorrect", HttpStatus.BAD_REQUEST)
        }
        if (request.currentPassword == request.newPassword) {
            throw BusinessException(HttpStatus.BAD_REQUEST.value(), "new password must differ from current", HttpStatus.BAD_REQUEST)
        }
        backendAccountRepository.save(
            BackendAccount {
                this.id = account.id
                this.username = account.username
                this.email = account.email
                this.phone = account.phone
                this.passwordHash = passwordHasher.encode(request.newPassword)
                this.displayName = account.displayName
                this.enabled = account.enabled
                this.createdTime = account.createdTime
                this.updatedTime = LocalDateTime.now(clock)
            },
        )
    }

    private fun ensureEmailAvailable(email: String, currentId: UUID, currentType: AccountType) {
        if (currentType == AccountType.USER && userRepository.findByEmail(email)?.id == currentId) return
        if (currentType == AccountType.BACKEND && backendAccountRepository.findByEmail(email)?.id == currentId) return
        val takenByUser = userRepository.findByEmail(email) != null
        val takenByBackend = backendAccountRepository.findByEmail(email) != null
        if (takenByUser || takenByBackend) {
            throw BusinessException(HttpStatus.CONFLICT.value(), "email already in use", HttpStatus.CONFLICT)
        }
    }

    private fun ensurePhoneAvailable(phone: String, currentId: UUID, currentType: AccountType) {
        if (currentType == AccountType.USER && userRepository.findByPhone(phone)?.id == currentId) return
        if (currentType == AccountType.BACKEND && backendAccountRepository.findByPhone(phone)?.id == currentId) return
        val takenByUser = userRepository.findByPhone(phone) != null
        val takenByBackend = backendAccountRepository.findByPhone(phone) != null
        if (takenByUser || takenByBackend) {
            throw BusinessException(HttpStatus.CONFLICT.value(), "phone already in use", HttpStatus.CONFLICT)
        }
    }

    private fun User.toResponse(accountType: AccountType): ProfileResponse = ProfileResponse(
        id = id,
        accountType = accountType,
        username = username,
        displayName = displayName,
        email = email,
        phone = phone,
        updatedTime = null,
    )

    private fun BackendAccount.toResponse(accountType: AccountType): ProfileResponse = ProfileResponse(
        id = id,
        accountType = accountType,
        username = username,
        displayName = displayName,
        email = email,
        phone = phone,
        updatedTime = updatedTime,
    )

    private fun notFound(resource: String): BusinessException =
        BusinessException(HttpStatus.NOT_FOUND.value(), "$resource not found", HttpStatus.NOT_FOUND)
}

package com.github.infrastructure.app.user.service

import com.github.infrastructure.app.user.BackendAccount
import com.github.infrastructure.app.user.BackendAccountResponse
import com.github.infrastructure.app.user.CreateBackendAccountRequest
import com.github.infrastructure.app.user.repository.BackendAccountRepository
import com.github.infrastructure.core.web.exception.BusinessException
import com.github.infrastructure.security.auth.LoginMode
import com.github.infrastructure.security.password.PasswordHasher
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.LocalDateTime
import java.util.UUID

@Service
class BackendAccountService(
    private val accountRepository: BackendAccountRepository,
    private val passwordHasher: PasswordHasher,
    private val clock: Clock,
) {
    @Transactional
    fun create(request: CreateBackendAccountRequest): BackendAccountResponse {
        if (accountRepository.findForLogin(LoginMode.USERNAME, request.username) != null) {
            throw BusinessException(HttpStatus.CONFLICT.value(), "backend username already exists", HttpStatus.CONFLICT)
        }
        val accountId = UUID.randomUUID()
        val now = LocalDateTime.now(clock)
        accountRepository.save(
            BackendAccount {
                id = accountId
                username = request.username
                email = request.email
                phone = request.phone
                passwordHash = passwordHasher.encode(request.password)
                displayName = request.displayName
                enabled = request.enabled
                createdTime = now
                updatedTime = now
            },
        )
        return BackendAccountResponse(
            id = accountId,
            username = request.username,
            email = request.email,
            phone = request.phone,
            displayName = request.displayName,
            enabled = request.enabled,
            roleIds = request.roleIds,
            createdTime = now,
            updatedTime = now,
        )
    }
}

package com.github.infrastructure.app.user

import com.github.infrastructure.security.auth.AccountType
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class BackendAccountController(
    private val service: BackendAccountService,
) {
    @PostMapping("/backend/accounts")
    @PreAuthorize("@permissionChecker.has('backend:account:write')")
    fun create(@Valid @RequestBody request: CreateBackendAccountRequest): BackendAccountResponse = service.create(request)
}

package com.github.infrastructure.app.user.controller

import com.github.infrastructure.app.user.dto.BackendAccountResponse
import com.github.infrastructure.app.user.dto.CreateBackendAccountRequest
import com.github.infrastructure.app.user.service.BackendAccountService
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

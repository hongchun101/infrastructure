package com.github.infrastructure.alert.controller

import com.github.infrastructure.alert.dto.AlertSilenceResponse
import com.github.infrastructure.alert.dto.CreateAlertSilenceRequest
import com.github.infrastructure.alert.service.AlertSilenceService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AlertSilenceController(
    private val silenceService: AlertSilenceService,
) {
    @GetMapping("/alert-silences")
    @PreAuthorize("@permissionChecker.has('alert:rule:read')")
    fun list(@RequestParam(required = false, defaultValue = "false") activeOnly: Boolean): List<AlertSilenceResponse> =
        if (activeOnly) silenceService.listActive() else silenceService.listAll()

    @PostMapping("/alert-silences")
    @PreAuthorize("@permissionChecker.has('alert:rule:write')")
    fun create(@Valid @RequestBody request: CreateAlertSilenceRequest): AlertSilenceResponse {
        val current = SecurityContextHolder.getContext().authentication?.name
        return silenceService.create(request, current)
    }

    @DeleteMapping("/alert-silences/{id}")
    @PreAuthorize("@permissionChecker.has('alert:rule:write')")
    fun deactivate(@PathVariable id: Long): AlertSilenceResponse =
        silenceService.deactivate(id)
}
package com.github.infrastructure.alert.controller

import com.github.infrastructure.alert.dto.AlertRuleResponse
import com.github.infrastructure.alert.dto.CreateAlertRuleRequest
import com.github.infrastructure.alert.dto.UpdateAlertRuleRequest
import com.github.infrastructure.alert.service.AlertRuleService
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class AlertRuleController(
    private val ruleService: AlertRuleService,
) {
    @GetMapping("/alert-rules")
    @PreAuthorize("@permissionChecker.has('alert:rule:read')")
    fun list(): List<AlertRuleResponse> = ruleService.list()

    @GetMapping("/alert-rules/{id}")
    @PreAuthorize("@permissionChecker.has('alert:rule:read')")
    fun get(@PathVariable id: UUID): AlertRuleResponse = ruleService.get(id)

    @PostMapping("/alert-rules")
    @PreAuthorize("@permissionChecker.has('alert:rule:write')")
    fun create(@Valid @RequestBody request: CreateAlertRuleRequest): AlertRuleResponse =
        ruleService.create(request)

    @PutMapping("/alert-rules/{id}")
    @PreAuthorize("@permissionChecker.has('alert:rule:write')")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAlertRuleRequest,
    ): AlertRuleResponse = ruleService.update(id, request)

    @DeleteMapping("/alert-rules/{id}")
    @PreAuthorize("@permissionChecker.has('alert:rule:write')")
    fun delete(@PathVariable id: UUID) {
        ruleService.delete(id)
    }
}

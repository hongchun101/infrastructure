package com.github.infrastructure.alert.controller

import com.github.infrastructure.alert.dto.AlertEventResponse
import com.github.infrastructure.alert.dto.AlertNotificationResponse
import com.github.infrastructure.alert.dto.PageResponse
import com.github.infrastructure.alert.entity.Severity
import com.github.infrastructure.alert.service.AlertEventService
import com.github.infrastructure.alert.service.AlertNotificationService
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class AlertEventController(
    private val eventService: AlertEventService,
    private val notificationService: AlertNotificationService,
) {
    @GetMapping("/alert-events")
    @PreAuthorize("@permissionChecker.has('alert:event:read')")
    fun list(
        @RequestParam(required = false) ruleId: UUID?,
        @RequestParam(required = false) severity: Severity?,
        @RequestParam(required = false) resolved: Boolean?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<AlertEventResponse> = eventService.list(ruleId, severity?.name, resolved, page, size)

    @GetMapping("/alert-events/{id}")
    @PreAuthorize("@permissionChecker.has('alert:event:read')")
    fun get(@PathVariable id: UUID): AlertEventResponse = eventService.get(id)

    @PostMapping("/alert-events/{id}/resolve")
    @PreAuthorize("@permissionChecker.has('alert:event:write')")
    fun resolve(@PathVariable id: UUID): AlertEventResponse = eventService.resolve(id)

    @GetMapping("/alert-events/{id}/notifications")
    @PreAuthorize("@permissionChecker.has('alert:event:read')")
    fun listNotifications(
        @PathVariable id: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): PageResponse<AlertNotificationResponse> = notificationService.list(id, page, size)
}

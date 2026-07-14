package com.github.infrastructure.notification.controller

import com.github.infrastructure.notification.dto.BatchSendResponse
import com.github.infrastructure.notification.dto.NotificationResponse
import com.github.infrastructure.notification.dto.SendBatchNotificationRequest
import com.github.infrastructure.notification.dto.SendNotificationRequest
import com.github.infrastructure.notification.dto.UnreadCountResponse
import com.github.infrastructure.notification.service.UserNotificationService
import com.github.infrastructure.security.context.CurrentUserContext
import jakarta.validation.Valid
import java.util.UUID
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/notifications")
class UserNotificationController(
    private val service: UserNotificationService,
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun list(
        @RequestParam(required = false, defaultValue = "false") unread: Boolean,
        @RequestParam(required = false, defaultValue = "false") includeArchived: Boolean,
        @RequestParam(required = false, defaultValue = "50") limit: Int,
    ): List<NotificationResponse> {
        val me = CurrentUserContext.require().id
        return service.listInbox(me, unread, includeArchived, limit)
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    fun unreadCount(): UnreadCountResponse {
        val me = CurrentUserContext.require().id
        return service.unreadCount(me)
    }

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    fun markRead(@PathVariable id: Long): NotificationResponse {
        val me = CurrentUserContext.require().id
        return service.markRead(id, me)
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    fun markAllRead(): Map<String, Int> {
        val me = CurrentUserContext.require().id
        val updated = service.markAllRead(me)
        return mapOf("updated" to updated)
    }

    @PostMapping("/{id}/archive")
    @PreAuthorize("isAuthenticated()")
    fun archive(@PathVariable id: Long): NotificationResponse {
        val me = CurrentUserContext.require().id
        return service.archive(id, me)
    }

    @PostMapping
    @PreAuthorize("@permissionChecker.has('notification:write')")
    fun send(@Valid @RequestBody request: SendNotificationRequest): NotificationResponse {
        val me = CurrentUserContext.require().id
        return service.sendToOne(me, request)
    }

    @PostMapping("/batch")
    @PreAuthorize("@permissionChecker.has('notification:write')")
    fun sendBatch(@Valid @RequestBody request: SendBatchNotificationRequest): BatchSendResponse =
        service.sendToMany(request)

    /**
     * Lets a privileged caller send a single notification to a specific recipient.
     * Distinct from the admin batch endpoint because it does not require admin permissions
     * beyond `notification:write` and is intended for system-side bridging (e.g. "you were
     * mentioned in a comment").
     */
    @PostMapping("/recipients/{recipientId}")
    @PreAuthorize("@permissionChecker.has('notification:write')")
    fun sendToRecipient(
        @PathVariable recipientId: UUID,
        @Valid @RequestBody request: SendNotificationRequest,
    ): NotificationResponse = service.sendToOne(recipientId, request)
}
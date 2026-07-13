package com.github.infrastructure.app.announcement.controller

import com.github.infrastructure.app.announcement.AnnouncementResponse
import com.github.infrastructure.app.announcement.AnnouncementStatus
import com.github.infrastructure.app.announcement.CreateAnnouncementRequest
import com.github.infrastructure.app.announcement.ScheduleAnnouncementRequest
import com.github.infrastructure.app.announcement.UpdateAnnouncementRequest
import com.github.infrastructure.app.announcement.service.AnnouncementService
import com.github.infrastructure.security.context.CurrentUserContext
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class AnnouncementController(
    private val announcementService: AnnouncementService,
) {
    @GetMapping("/announcements")
    @PreAuthorize("@permissionChecker.has('announcement:read')")
    fun list(
        @RequestParam(required = false) status: AnnouncementStatus?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false, defaultValue = "false") unreadOnly: Boolean,
    ): List<AnnouncementResponse> =
        announcementService.list(status, keyword, unreadOnly, CurrentUserContext.require())

    @GetMapping("/announcements/{id}")
    @PreAuthorize("@permissionChecker.has('announcement:read')")
    fun get(@PathVariable id: UUID): AnnouncementResponse =
        announcementService.get(id, CurrentUserContext.require())

    @PostMapping("/announcements")
    @PreAuthorize("@permissionChecker.has('announcement:write')")
    fun create(
        @Valid @RequestBody request: CreateAnnouncementRequest,
    ): AnnouncementResponse =
        announcementService.create(request, CurrentUserContext.require())

    @PutMapping("/announcements/{id}")
    @PreAuthorize("@permissionChecker.has('announcement:write')")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateAnnouncementRequest,
    ): AnnouncementResponse =
        announcementService.update(id, request, CurrentUserContext.require())

    @PostMapping("/announcements/{id}/publish")
    @PreAuthorize("@permissionChecker.has('announcement:write')")
    fun publish(@PathVariable id: UUID): AnnouncementResponse =
        announcementService.publish(id, CurrentUserContext.require())

    @PostMapping("/announcements/{id}/archive")
    @PreAuthorize("@permissionChecker.has('announcement:write')")
    fun archive(@PathVariable id: UUID): AnnouncementResponse =
        announcementService.archive(id, CurrentUserContext.require())

    @PostMapping("/announcements/{id}/read")
    @PreAuthorize("@permissionChecker.has('announcement:read')")
    fun markRead(@PathVariable id: UUID): AnnouncementResponse =
        announcementService.markRead(id, CurrentUserContext.require())

    @PostMapping("/announcements/{id}/schedule")
    @PreAuthorize("@permissionChecker.has('announcement:write')")
    fun schedule(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ScheduleAnnouncementRequest,
    ): AnnouncementResponse =
        announcementService.schedule(id, request.publishAt, CurrentUserContext.require())

    @DeleteMapping("/announcements/{id}/schedule")
    @PreAuthorize("@permissionChecker.has('announcement:write')")
    fun clearSchedule(@PathVariable id: UUID): AnnouncementResponse =
        announcementService.clearSchedule(id, CurrentUserContext.require())

    @DeleteMapping("/announcements/{id}")
    @PreAuthorize("@permissionChecker.has('announcement:write')")
    fun delete(@PathVariable id: UUID) {
        announcementService.delete(id)
    }
}

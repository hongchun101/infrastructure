package com.github.infrastructure.scheduler.controller

import com.github.infrastructure.scheduler.dto.JobDefinitionResponse
import com.github.infrastructure.scheduler.dto.JobExecutionResponse
import com.github.infrastructure.scheduler.dto.TriggerJobRequest
import com.github.infrastructure.scheduler.dto.UpdateScheduleRequest
import com.github.infrastructure.scheduler.service.JobAdminService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/admin/jobs")
class JobAdminController(
    private val adminService: JobAdminService,
) {
    @GetMapping
    @PreAuthorize("@permissionChecker.has('job:read')")
    fun list(): List<JobDefinitionResponse> = adminService.list()

    @GetMapping("/{code}")
    @PreAuthorize("@permissionChecker.has('job:read')")
    fun get(@PathVariable code: String): JobDefinitionResponse = adminService.get(code)

    @GetMapping("/{code}/runs")
    @PreAuthorize("@permissionChecker.has('job:read')")
    fun runs(
        @PathVariable code: String,
        @RequestParam(required = false, defaultValue = "50") limit: Int,
    ): List<JobExecutionResponse> = adminService.listRuns(code, limit)

    @PostMapping("/{code}/pause")
    @PreAuthorize("@permissionChecker.has('job:write')")
    fun pause(@PathVariable code: String): JobDefinitionResponse = adminService.pause(code)

    @PostMapping("/{code}/resume")
    @PreAuthorize("@permissionChecker.has('job:write')")
    fun resume(@PathVariable code: String): JobDefinitionResponse = adminService.resume(code)

    @PutMapping("/{code}/schedule")
    @PreAuthorize("@permissionChecker.has('job:write')")
    fun updateSchedule(
        @PathVariable code: String,
        @Valid @RequestBody request: UpdateScheduleRequest,
    ): JobDefinitionResponse = adminService.updateSchedule(code, request.toSchedule())

    @PostMapping("/{code}/trigger")
    @PreAuthorize("@permissionChecker.has('job:write')")
    fun trigger(
        @PathVariable code: String,
        @RequestBody(required = false) request: TriggerJobRequest?,
    ): JobExecutionResponse = adminService.trigger(code, request ?: TriggerJobRequest())
}